package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.FilePathResolver
import com.pumpkin.log.util.ObjectMapperFactory
import java.io.Closeable
import java.io.File
import java.io.FileWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class AsyncFileLogAppender(
    filePath: String? = null,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val createDirectories: Boolean = true
) : LogAppender, Closeable {

    private val resolvedFilePath: String = FilePathResolver.resolve(filePath)
    private val queue = LinkedBlockingQueue<HttpLog>(bufferSize)
    private val running = AtomicBoolean(true)
    private val objectMapper = ObjectMapperFactory.instance
    private val worker: Thread
    private val shutdownHook: Thread

    private val _droppedCount = AtomicLong(0)
    private val _writtenCount = AtomicLong(0)

    val droppedCount: Long get() = _droppedCount.get()
    val writtenCount: Long get() = _writtenCount.get()

    var onDropped: ((HttpLog) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null

    init {
        validateAndPrepareDirectory()
        worker = createWorkerThread()
        shutdownHook = Thread({ shutdown() }, SHUTDOWN_HOOK_THREAD_NAME)
        Runtime.getRuntime().addShutdownHook(shutdownHook)
    }

    private fun validateAndPrepareDirectory() {
        val parentDir = File(resolvedFilePath).parentFile ?: return
        if (parentDir.exists()) return

        if (createDirectories) {
            require(parentDir.mkdirs()) {
                "Failed to create directory: ${parentDir.absolutePath}"
            }
        } else {
            throw IllegalArgumentException("Directory does not exist: ${parentDir.absolutePath}")
        }
    }

    private fun createWorkerThread(): Thread =
        Thread({ processQueue() }, WORKER_THREAD_NAME).apply {
            isDaemon = true
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
                onError?.invoke(e)
            }
            start()
        }

    override fun append(log: HttpLog) {
        if (!queue.offer(log)) {
            _droppedCount.incrementAndGet()
            onDropped?.invoke(log)
        }
    }

    private fun processQueue() {
        val batch = mutableListOf<HttpLog>()

        try {
            FileWriter(resolvedFilePath, true).buffered().use { writer ->
                while (running.get() || queue.isNotEmpty()) {
                    try {
                        drainBatch(batch)
                        if (batch.isNotEmpty()) {
                            writeBatch(writer, batch)
                        }
                    } catch (_: InterruptedException) {
                        return
                    } catch (e: Exception) {
                        handleWriteError(e, batch)
                    }
                }
                writer.flush()
            }
        } catch (e: Exception) {
            onError?.invoke(e)
        }
    }

    private fun drainBatch(batch: MutableList<HttpLog>) {
        queue.drainTo(batch, batchSize)
        if (batch.isEmpty()) {
            queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)?.let { log ->
                batch.add(log)
                queue.drainTo(batch, batchSize - 1)
            }
        }
    }

    private fun writeBatch(writer: java.io.BufferedWriter, batch: MutableList<HttpLog>) {
        batch.forEach { log ->
            writer.appendLine(objectMapper.writeValueAsString(log))
        }
        _writtenCount.addAndGet(batch.size.toLong())
        batch.clear()
        writer.flush()
    }

    private fun handleWriteError(e: Exception, batch: MutableList<HttpLog>) {
        onError?.invoke(e)
        if (batch.isNotEmpty()) {
            _droppedCount.addAndGet(batch.size.toLong())
            batch.clear()
        }
        Thread.sleep(ERROR_RETRY_DELAY_MS)
    }

    fun shutdown() {
        if (!running.compareAndSet(true, false)) return

        worker.join(SHUTDOWN_TIMEOUT_MS)
        if (worker.isAlive) worker.interrupt()

        runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
    }

    override fun close() = shutdown()

    companion object {
        const val DEFAULT_BUFFER_SIZE = 10_000
        const val DEFAULT_BATCH_SIZE = 100

        private const val POLL_TIMEOUT_MS = 100L
        private const val ERROR_RETRY_DELAY_MS = 100L
        private const val SHUTDOWN_TIMEOUT_MS = 5000L
        private const val WORKER_THREAD_NAME = "async-file-log-worker"
        private const val SHUTDOWN_HOOK_THREAD_NAME = "async-file-log-shutdown-hook"
    }
}
