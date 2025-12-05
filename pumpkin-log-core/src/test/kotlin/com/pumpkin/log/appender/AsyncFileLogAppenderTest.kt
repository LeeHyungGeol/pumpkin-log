package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.ObjectMapperFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

class AsyncFileLogAppenderTest {

    @TempDir
    lateinit var tempDir: Path

    private val objectMapper = ObjectMapperFactory.instance
    private val pid = ProcessHandle.current().pid()

    private fun createTestLog(index: Int = 1): HttpLog {
        return HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test/$index",
            timestamp = Instant.now()
        )
    }

    @Test
    fun `should write log to file asynchronously`() {
        val filePath = tempDir.resolve("test.{pid}.jsonl").toString()
        val appender = AsyncFileLogAppender(filePath = filePath)

        appender.append(createTestLog())

        val actualFile = tempDir.resolve("test.$pid.jsonl").toFile()
        await.atMost(Duration.ofSeconds(2)).untilAsserted {
            assertThat(actualFile.readLines()).hasSize(1)
        }

        appender.shutdown()
    }

    @Test
    fun `should write valid JSON on each line`() {
        val filePath = tempDir.resolve("valid.{pid}.jsonl").toString()
        val appender = AsyncFileLogAppender(filePath = filePath)

        repeat(5) { appender.append(createTestLog(it)) }

        val actualFile = tempDir.resolve("valid.$pid.jsonl").toFile()
        await.atMost(Duration.ofSeconds(2)).untilAsserted {
            assertThat(actualFile.readLines()).hasSize(5)
        }

        actualFile.readLines().forEach { line ->
            val parsed = objectMapper.readTree(line)
            assertThat(parsed.has("type")).isTrue()
            assertThat(parsed.has("http_path")).isTrue()
        }

        appender.shutdown()
    }

    @Test
    fun `should match writtenCount with number of written logs`() {
        val filePath = tempDir.resolve("count.{pid}.jsonl").toString()
        val appender = AsyncFileLogAppender(filePath = filePath)

        repeat(10) { appender.append(createTestLog(it)) }

        await.atMost(Duration.ofSeconds(2)).untilAsserted {
            assertThat(appender.writtenCount).isEqualTo(10)
        }

        appender.shutdown()
    }

    @Test
    fun `should process logs in batches according to batchSize`() {
        val filePath = tempDir.resolve("batch.{pid}.jsonl").toString()
        val appender = AsyncFileLogAppender(
            filePath = filePath,
            batchSize = 10
        )

        repeat(20) { appender.append(createTestLog(it)) }
        appender.shutdown()

        val actualFile = tempDir.resolve("batch.$pid.jsonl").toFile()
        assertThat(actualFile.readLines()).hasSize(20)
        assertThat(appender.writtenCount).isEqualTo(20)
    }

    @Test
    fun `should increment droppedCount and call onDropped when buffer overflows`() {
        val filePath = tempDir.resolve("drop.{pid}.jsonl").toString()
        val droppedLogs = mutableListOf<HttpLog>()

        val appender = AsyncFileLogAppender(
            filePath = filePath,
            bufferSize = 1,
            batchSize = 100
        ).apply {
            onDropped = { droppedLogs.add(it) }
        }

        repeat(20) { appender.append(createTestLog(it)) }

        await.atMost(Duration.ofSeconds(2)).untilAsserted {
            assertThat(appender.droppedCount).isGreaterThan(0)
            assertThat(droppedLogs).isNotEmpty()
        }

        appender.shutdown()
    }

    @Test
    fun `should create directory automatically when createDirectories is true`() {
        val nestedPath = tempDir.resolve("nested/deep/dir/log.{pid}.jsonl").toString()
        val appender = AsyncFileLogAppender(filePath = nestedPath, createDirectories = true)

        appender.append(createTestLog())

        val actualFile = tempDir.resolve("nested/deep/dir/log.$pid.jsonl").toFile()
        await.atMost(Duration.ofSeconds(2)).untilAsserted {
            assertThat(actualFile).exists()
        }

        appender.shutdown()
    }

    @Test
    fun `should throw exception when directory not exists and createDirectories is false`() {
        val invalidPath = tempDir.resolve("non-existent-dir/fail.jsonl").toString()

        assertThatThrownBy {
            AsyncFileLogAppender(filePath = invalidPath, createDirectories = false)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Directory does not exist")
    }

    @Test
    fun `should throw exception when directory creation fails`() {
        val readOnlyDir = tempDir.resolve("readonly").toFile()
        readOnlyDir.mkdir()
        readOnlyDir.setWritable(false)

        val nestedPath = "${readOnlyDir.absolutePath}/nested/log.{pid}.jsonl"

        assertThatThrownBy {
            AsyncFileLogAppender(filePath = nestedPath, createDirectories = true)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Failed to create directory")

        readOnlyDir.setWritable(true)
    }

    @Test
    fun `should flush remaining logs on shutdown`() {
        val filePath = tempDir.resolve("shutdown.{pid}.jsonl").toString()
        val appender = AsyncFileLogAppender(filePath = filePath)

        repeat(100) { appender.append(createTestLog(it)) }
        appender.shutdown()

        val actualFile = tempDir.resolve("shutdown.$pid.jsonl").toFile()
        assertThat(actualFile.readLines()).hasSize(100)
        assertThat(appender.writtenCount).isEqualTo(100)
    }

    @Test
    fun `should be safe to call shutdown twice`() {
        val filePath = tempDir.resolve("double.{pid}.jsonl").toString()
        val appender = AsyncFileLogAppender(filePath = filePath)

        appender.append(createTestLog())

        assertThatCode {
            appender.shutdown()
            appender.shutdown()
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should auto insert PID when no placeholder specified`() {
        val filePath = tempDir.resolve("auto.jsonl").toString()
        val appender = AsyncFileLogAppender(filePath = filePath)

        appender.append(createTestLog())

        val actualFile = tempDir.resolve("auto.$pid.jsonl").toFile()
        await.atMost(Duration.ofSeconds(2)).untilAsserted {
            assertThat(actualFile).exists()
        }

        appender.shutdown()
    }
}
