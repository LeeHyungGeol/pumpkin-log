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

/**
 * 로그를 파일에 JSONL 형식으로 비동기 기록하는 Appender.
 *
 * 별도의 Worker Thread가 큐에서 로그를 꺼내 배치로 파일에 기록합니다.
 * 호출 스레드는 큐에 offer만 하고 즉시 반환되어 응답 지연이 최소화됩니다.
 *
 * @param filePath 로그 파일 경로 (null이면 기본 경로 사용, `{pid}` 플레이스홀더 지원)
 * @param bufferSize 큐 버퍼 크기 (기본값: 10,000)
 * @param batchSize 한 번에 파일에 쓰는 로그 수 (기본값: 100)
 * @see FileLogAppender 저트래픽 환경을 위한 동기 구현
 */
class AsyncFileLogAppender(
    filePath: String? = null,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
) : LogAppender, Closeable {

    private val resolvedFilePath: String = FilePathResolver.resolve(filePath)
    private val queue = LinkedBlockingQueue<HttpLog>(bufferSize)
    private val running = AtomicBoolean(true)
    private val objectMapper = ObjectMapperFactory.instance
    private val worker: Thread
    private val shutdownHook: Thread

    private val _droppedCount = AtomicLong(0)
    private val _writtenCount = AtomicLong(0)

    /** 큐 초과로 유실된 로그 수 */
    val droppedCount: Long get() = _droppedCount.get()

    /** 파일에 성공적으로 기록된 로그 수 */
    val writtenCount: Long get() = _writtenCount.get()

    /** 로그 유실 시 호출되는 콜백 */
    var onDropped: ((HttpLog) -> Unit)? = null

    /** 런타임 오류 발생 시 호출되는 콜백 */
    var onError: ((Throwable) -> Unit)? = null

    init {
        validateDirectory()
        worker = createWorkerThread()
        shutdownHook = Thread({ shutdown() }, SHUTDOWN_HOOK_THREAD_NAME)
        Runtime.getRuntime().addShutdownHook(shutdownHook)
    }

    private fun validateDirectory() {
        File(resolvedFilePath).parentFile?.let { parentDir ->
            require(parentDir.exists()) {
                "Directory does not exist: ${parentDir.absolutePath}"
            }
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

    /**
     * Worker Thread를 종료하고 리소스를 정리합니다.
     *
     * 최대 5초간 Worker Thread의 종료를 대기하며,
     * 시간 내 종료되지 않으면 interrupt를 발생시킵니다.
     */
    fun shutdown() {
        if (!running.compareAndSet(true, false)) return

        worker.join(SHUTDOWN_TIMEOUT_MS)
        if (worker.isAlive) worker.interrupt()

        runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
    }

    override fun close() = shutdown()

    companion object {
        /** 기본 큐 버퍼 크기 */
        const val DEFAULT_BUFFER_SIZE = 10_000

        /** 기본 배치 쓰기 크기 */
        const val DEFAULT_BATCH_SIZE = 100

        private const val POLL_TIMEOUT_MS = 100L
        private const val ERROR_RETRY_DELAY_MS = 100L
        private const val SHUTDOWN_TIMEOUT_MS = 5000L
        private const val WORKER_THREAD_NAME = "async-file-log-worker"
        private const val SHUTDOWN_HOOK_THREAD_NAME = "async-file-log-shutdown-hook"
    }
}
