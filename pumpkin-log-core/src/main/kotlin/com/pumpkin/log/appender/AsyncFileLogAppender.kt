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
    private val bufferSize: Int = 1000,
    private val batchSize: Int = 100,
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
    val queueSize: Int get() = queue.size

    var onDropped: ((HttpLog) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null

    init {
        val file = File(resolvedFilePath)
        val parentDir = file.parentFile
        require(parentDir == null || parentDir.exists()) {
            "Directory does not exist: ${parentDir?.absolutePath}"
        }

        worker = Thread({ processQueue() }, "async-file-log-worker").apply {
            isDaemon = true
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
                onError?.invoke(e)
            }
            start()
        }

        shutdownHook = Thread({ shutdown() }, "async-file-log-shutdown-hook")
        Runtime.getRuntime().addShutdownHook(shutdownHook)
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
                        queue.drainTo(batch, batchSize)

                        if (batch.isEmpty()) {
                            val log = queue.poll(100, TimeUnit.MILLISECONDS)
                            if (log != null) {
                                batch.add(log)
                                queue.drainTo(batch, batchSize - 1)
                            }
                        }

                        if (batch.isNotEmpty()) {
                            batch.forEach { log ->
                                writer.appendLine(objectMapper.writeValueAsString(log))
                            }
                            _writtenCount.addAndGet(batch.size.toLong())
                            batch.clear()
                            writer.flush()
                        }
                    } catch (e: InterruptedException) {
                        return
                    } catch (e: Exception) {
                        onError?.invoke(e)
                        if (batch.isNotEmpty()) {
                            _droppedCount.addAndGet(batch.size.toLong())
                            batch.clear()
                        }
                        Thread.sleep(100)
                    }
                }
                writer.flush()
            }
        } catch (e: Exception) {
            onError?.invoke(e)
        }
    }

    fun shutdown() {
        if (!running.compareAndSet(true, false)) return

        worker.join(5000)
        if (worker.isAlive) worker.interrupt()

        runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
    }

    override fun close() = shutdown()
}
