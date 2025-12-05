package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.FilePathResolver
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@Disabled("Performance test - run manually")
class AppenderPerformanceTest {

    @TempDir
    lateinit var tempDir: Path

    private fun createTestLog(index: Int): HttpLog {
        return HttpLog(
            type = "log.v1.http",
            userAgent = "PerformanceTest/1.0",
            duration = index.toLong(),
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/api/test/$index",
            httpQuery = "index=$index",
            extra = mapOf("index" to index),
            timestamp = Instant.now()
        )
    }

    @Test
    fun `sync appender throughput - 10,000 logs`() {
        val logFile = tempDir.resolve("sync-throughput.jsonl").toString()
        val appender = FileLogAppender(logFile)
        val count = 10_000

        val elapsed = measureTimeMillis {
            repeat(count) { i ->
                appender.append(createTestLog(i))
            }
        }

        val throughput = count * 1000.0 / elapsed
        val actualFilePath = FilePathResolver.resolve(logFile)
        val fileSize = File(actualFilePath).length()

        println("""
            |=== Sync Appender Throughput ===
            |Logs written: $count
            |Elapsed: ${elapsed}ms
            |Throughput: ${"%.2f".format(throughput)} logs/sec
            |File size: ${fileSize / 1024}KB
        """.trimMargin())
    }

    @Test
    fun `async appender single producer throughput - 100,000 logs`() {
        val logFile = tempDir.resolve("async-single-throughput.jsonl").toString()
        val appender = AsyncFileLogAppender(
            filePath = logFile,
            bufferSize = 100_000,
            batchSize = 500
        )
        val count = 100_000

        val elapsed = measureTimeMillis {
            repeat(count) { i ->
                appender.append(createTestLog(i))
            }
            appender.shutdown()
        }

        val throughput = count * 1000.0 / elapsed
        val actualFilePath = FilePathResolver.resolve(logFile)
        val fileSize = File(actualFilePath).length()

        println("""
            |=== Async Appender Single Producer Throughput ===
            |Logs submitted: $count
            |Logs written: ${appender.writtenCount}
            |Dropped: ${appender.droppedCount}
            |Elapsed: ${elapsed}ms
            |Throughput: ${"%.2f".format(throughput)} logs/sec
            |File size: ${fileSize / 1024}KB
        """.trimMargin())
    }

    @Test
    fun `async appender multi producer throughput - 10 threads x 10,000 logs`() {
        val logFile = tempDir.resolve("async-multi-throughput.jsonl").toString()
        val appender = AsyncFileLogAppender(
            filePath = logFile,
            bufferSize = 100_000,
            batchSize = 500
        )
        val threadCount = 10
        val logsPerThread = 10_000
        val totalLogs = threadCount * logsPerThread
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val submitted = AtomicInteger(0)

        val elapsed = measureTimeMillis {
            repeat(threadCount) { threadIdx ->
                executor.submit {
                    try {
                        repeat(logsPerThread) { i ->
                            appender.append(createTestLog(threadIdx * logsPerThread + i))
                            submitted.incrementAndGet()
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            appender.shutdown()
        }

        executor.shutdown()
        val throughput = totalLogs * 1000.0 / elapsed
        val actualFilePath = FilePathResolver.resolve(logFile)
        val fileSize = File(actualFilePath).length()

        println("""
            |=== Async Appender Multi Producer Throughput ===
            |Producer threads: $threadCount
            |Logs per thread: $logsPerThread
            |Total submitted: ${submitted.get()}
            |Logs written: ${appender.writtenCount}
            |Dropped: ${appender.droppedCount}
            |Elapsed: ${elapsed}ms
            |Throughput: ${"%.2f".format(throughput)} logs/sec
            |File size: ${fileSize / 1024}KB
        """.trimMargin())
    }

    @Test
    fun `async appender buffer overflow - small buffer causes drops`() {
        val logFile = tempDir.resolve("async-overflow.jsonl").toString()
        val bufferSize = 100
        val droppedLogs = AtomicInteger(0)

        val appender = AsyncFileLogAppender(
            filePath = logFile,
            bufferSize = bufferSize,
            batchSize = 10
        ).apply {
            onDropped = { droppedLogs.incrementAndGet() }
        }

        val count = 10_000

        val elapsed = measureTimeMillis {
            repeat(count) { i ->
                appender.append(createTestLog(i))
            }
            appender.shutdown()
        }

        val dropRate = appender.droppedCount * 100.0 / count

        println("""
            |=== Async Appender Buffer Overflow ===
            |Buffer size: $bufferSize
            |Logs submitted: $count
            |Logs written: ${appender.writtenCount}
            |Dropped: ${appender.droppedCount}
            |Drop rate: ${"%.2f".format(dropRate)}%
            |Elapsed: ${elapsed}ms
            |Callback invocations: ${droppedLogs.get()}
        """.trimMargin())
    }

    @Test
    fun `find async appender throughput ceiling`() {
        println("=== Finding Async Appender Throughput Ceiling ===\n")

        val testCases = listOf(1_000, 10_000, 50_000, 100_000, 200_000, 500_000)

        testCases.forEach { count ->
            val logFile = tempDir.resolve("async-ceiling-$count.jsonl").toString()
            val appender = AsyncFileLogAppender(
                filePath = logFile,
                bufferSize = count,
                batchSize = 1000
            )

            val elapsed = measureTimeMillis {
                repeat(count) { i ->
                    appender.append(createTestLog(i))
                }
                appender.shutdown()
            }

            val throughput = count * 1000.0 / elapsed

            println("""
                |[$count logs] Elapsed: ${elapsed}ms, Throughput: ${"%.0f".format(throughput)} logs/sec, Written: ${appender.writtenCount}, Dropped: ${appender.droppedCount}
            """.trimMargin())

            // cleanup
            File(logFile).delete()
        }
    }

    @Test
    fun `sync vs async comparison - 50,000 logs`() {
        val count = 50_000

        // Sync test
        val syncFile = tempDir.resolve("compare-sync.jsonl").toString()
        val syncAppender = FileLogAppender(syncFile)

        val syncElapsed = measureTimeMillis {
            repeat(count) { i ->
                syncAppender.append(createTestLog(i))
            }
        }

        // Async test
        val asyncFile = tempDir.resolve("compare-async.jsonl").toString()
        val asyncAppender = AsyncFileLogAppender(
            filePath = asyncFile,
            bufferSize = count,
            batchSize = 100
        )

        val asyncElapsed = measureTimeMillis {
            repeat(count) { i ->
                asyncAppender.append(createTestLog(i))
            }
            asyncAppender.shutdown()
        }

        val syncThroughput = count * 1000.0 / syncElapsed
        val asyncThroughput = count * 1000.0 / asyncElapsed
        val improvement = (asyncThroughput - syncThroughput) / syncThroughput * 100

        println("""
            |=== Sync vs Async Comparison ===
            |Log count: $count
            |
            |Sync Appender:
            |  Elapsed: ${syncElapsed}ms
            |  Throughput: ${"%.2f".format(syncThroughput)} logs/sec
            |
            |Async Appender:
            |  Elapsed: ${asyncElapsed}ms
            |  Throughput: ${"%.2f".format(asyncThroughput)} logs/sec
            |  Dropped: ${asyncAppender.droppedCount}
            |
            |Improvement: ${"%.2f".format(improvement)}%
        """.trimMargin())
    }

    @Test
    fun `producer scaling test - thread count impact on throughput`() {
        println("=== Producer Scaling Test ===")
        println("Purpose: Verify AsyncFileLogAppender handles 100+ concurrent requests in microservice environment\n")

        val threadCounts = listOf(10, 50, 100, 200, 500)
        val logsPerThread = 10_000
        val bufferSize = 500_000

        println("%-10s | %-12s | %-10s | %-15s | %-10s".format(
            "Threads", "Total Logs", "Elapsed", "Throughput", "Dropped"
        ))
        println("-".repeat(65))

        threadCounts.forEach { threadCount ->
            val totalLogs = threadCount * logsPerThread
            val logFile = tempDir.resolve("scaling-$threadCount.jsonl").toString()
            val appender = AsyncFileLogAppender(
                filePath = logFile,
                bufferSize = bufferSize,
                batchSize = 500
            )
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)

            val elapsed = measureTimeMillis {
                repeat(threadCount) { threadIdx ->
                    executor.submit {
                        try {
                            repeat(logsPerThread) { i ->
                                appender.append(createTestLog(threadIdx * logsPerThread + i))
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }
                latch.await()
                appender.shutdown()
            }

            executor.shutdown()
            val throughput = totalLogs * 1000.0 / elapsed

            println("%-10d | %-12d | %-10s | %-15s | %-10d".format(
                threadCount,
                totalLogs,
                "${elapsed}ms",
                "${"%.0f".format(throughput)} logs/s",
                appender.droppedCount
            ))

            // cleanup
            val actualFilePath = FilePathResolver.resolve(logFile)
            File(actualFilePath).delete()
        }
    }

    @Test
    fun `buffer and batch size optimization test`() {
        println("=== Buffer/Batch Size Optimization Test ===")
        println("Purpose: Find optimal bufferSize/batchSize combination\n")

        val bufferSizes = listOf(1_000, 10_000, 50_000, 100_000)
        val batchSizes = listOf(10, 50, 100, 500, 1_000)
        val logCount = 100_000

        println("%-12s | %-10s | %-10s | %-15s | %-10s".format(
            "BufferSize", "BatchSize", "Elapsed", "Throughput", "Dropped"
        ))
        println("-".repeat(65))

        bufferSizes.forEach { bufferSize ->
            batchSizes.forEach { batchSize ->
                val logFile = tempDir.resolve("optimize-$bufferSize-$batchSize.jsonl").toString()
                val appender = AsyncFileLogAppender(
                    filePath = logFile,
                    bufferSize = bufferSize,
                    batchSize = batchSize
                )

                val elapsed = measureTimeMillis {
                    repeat(logCount) { i ->
                        appender.append(createTestLog(i))
                    }
                    appender.shutdown()
                }

                val throughput = logCount * 1000.0 / elapsed

                println("%-12d | %-10d | %-10s | %-15s | %-10d".format(
                    bufferSize,
                    batchSize,
                    "${elapsed}ms",
                    "${"%.0f".format(throughput)} logs/s",
                    appender.droppedCount
                ))

                // cleanup
                val actualFilePath = FilePathResolver.resolve(logFile)
                File(actualFilePath).delete()
            }
        }
    }

    @Test
    fun `memory pressure test - heap usage under load`() {
        println("=== Memory Pressure Test ===")
        println("Purpose: Track heap memory usage during large log processing, identify OOM threshold\n")

        val logCounts = listOf(100_000, 500_000, 1_000_000, 2_000_000)
        val runtime = Runtime.getRuntime()

        println("%-12s | %-15s | %-15s | %-15s | %-15s".format(
            "Logs", "Heap Before", "Heap After", "Heap Delta", "Throughput"
        ))
        println("-".repeat(80))

        logCounts.forEach { logCount ->
            // Force GC before measurement
            System.gc()
            Thread.sleep(100)

            val heapBefore = runtime.totalMemory() - runtime.freeMemory()

            val logFile = tempDir.resolve("memory-$logCount.jsonl").toString()
            val appender = AsyncFileLogAppender(
                filePath = logFile,
                bufferSize = logCount.coerceAtMost(500_000),
                batchSize = 1000
            )

            val elapsed = measureTimeMillis {
                repeat(logCount) { i ->
                    appender.append(createTestLog(i))
                }
                appender.shutdown()
            }

            val heapAfter = runtime.totalMemory() - runtime.freeMemory()
            val heapDelta = heapAfter - heapBefore
            val throughput = logCount * 1000.0 / elapsed

            println("%-12d | %-15s | %-15s | %-15s | %-15s".format(
                logCount,
                "${heapBefore / 1024 / 1024}MB",
                "${heapAfter / 1024 / 1024}MB",
                "${heapDelta / 1024 / 1024}MB",
                "${"%.0f".format(throughput)} logs/s"
            ))

            // cleanup
            val actualFilePath = FilePathResolver.resolve(logFile)
            File(actualFilePath).delete()
        }
    }
}
