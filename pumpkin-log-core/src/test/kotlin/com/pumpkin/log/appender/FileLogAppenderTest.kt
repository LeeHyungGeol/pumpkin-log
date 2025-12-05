package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.FilePathResolver
import com.pumpkin.log.util.ObjectMapperFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Instant

class FileLogAppenderTest {

    @TempDir
    lateinit var tempDir: Path

    private val objectMapper = ObjectMapperFactory.instance
    private val pid = ProcessHandle.current().pid()

    @Test
    fun `should create file if not exists`() {
        // given
        val filePath = tempDir.resolve("new-log.{pid}.jsonl").toString()
        val resolvedPath = FilePathResolver.resolve(filePath)
        val appender = FileLogAppender(filePath)
        val log = createTestLog()

        // when
        appender.append(log)

        // then
        val file = File(resolvedPath)
        assertThat(file).exists()
    }

    @Test
    fun `should append log in JSONL format`() {
        // given
        val filePath = tempDir.resolve("test.{pid}.jsonl").toString()
        val resolvedPath = FilePathResolver.resolve(filePath)
        val appender = FileLogAppender(filePath)
        val log = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/health",
            timestamp = Instant.parse("2024-01-15T10:00:00Z")
        )

        // when
        appender.append(log)

        // then
        val lines = File(resolvedPath).readLines()
        assertThat(lines).hasSize(1)
        assertThat(lines[0]).startsWith("{")
        assertThat(lines[0]).endsWith("}")
        assertThat(lines[0]).contains("\"user_agent\":\"TestAgent\"")
    }

    @Test
    fun `should append multiple logs as separate lines`() {
        // given
        val filePath = tempDir.resolve("multi.{pid}.jsonl").toString()
        val resolvedPath = FilePathResolver.resolve(filePath)
        val appender = FileLogAppender(filePath)
        val logs = listOf(
            createTestLog(path = "/api/users", statusCode = 200),
            createTestLog(path = "/api/orders", statusCode = 201),
            createTestLog(path = "/api/products", statusCode = 404)
        )

        // when
        logs.forEach { appender.append(it) }

        // then
        val lines = File(resolvedPath).readLines()
        assertThat(lines).hasSize(3)
    }

    @Test
    fun `should write valid JSON on each line`() {
        // given
        val filePath = tempDir.resolve("valid-json.{pid}.jsonl").toString()
        val resolvedPath = FilePathResolver.resolve(filePath)
        val appender = FileLogAppender(filePath)
        val logs = listOf(
            createTestLog(path = "/first"),
            createTestLog(path = "/second"),
            createTestLog(path = "/third")
        )

        // when
        logs.forEach { appender.append(it) }

        // then
        val lines = File(resolvedPath).readLines()
        lines.forEach { line ->
            val parsed = objectMapper.readTree(line)
            assertThat(parsed.has("type")).isTrue()
            assertThat(parsed.has("user_agent")).isTrue()
            assertThat(parsed.has("http_path")).isTrue()
            assertThat(parsed.has("timestamp")).isTrue()
        }
    }

    @Test
    fun `should append to existing file`() {
        // given
        val filePath = tempDir.resolve("existing.{pid}.jsonl").toString()
        val resolvedPath = FilePathResolver.resolve(filePath)
        val file = File(resolvedPath)
        file.writeText("{\"existing\":\"data\"}\n")

        val appender = FileLogAppender(filePath)
        val log = createTestLog()

        // when
        appender.append(log)

        // then
        val lines = file.readLines()
        assertThat(lines).hasSize(2)
        assertThat(lines[0]).isEqualTo("{\"existing\":\"data\"}")
        assertThat(lines[1]).contains("\"type\":\"log.v1.http\"")
    }

    @Test
    fun `should preserve field values in file`() {
        // given
        val filePath = tempDir.resolve("values.{pid}.jsonl").toString()
        val resolvedPath = FilePathResolver.resolve(filePath)
        val appender = FileLogAppender(filePath)
        val log = HttpLog(
            userAgent = "Mozilla/5.0",
            duration = 250,
            httpStatusCode = 500,
            httpMethod = "POST",
            httpPath = "/api/submit",
            httpQuery = "retry=true",
            extra = mapOf("errorCode" to "E001"),
            timestamp = Instant.parse("2024-06-01T12:30:45.123Z")
        )

        // when
        appender.append(log)

        // then
        val content = File(resolvedPath).readText()
        assertThat(content).contains("\"user_agent\":\"Mozilla/5.0\"")
        assertThat(content).contains("\"duration\":250")
        assertThat(content).contains("\"http_status_code\":500")
        assertThat(content).contains("\"http_method\":\"POST\"")
        assertThat(content).contains("\"http_path\":\"/api/submit\"")
        assertThat(content).contains("\"http_query\":\"retry=true\"")
        assertThat(content).contains("\"extra\"")
        assertThat(content).contains("\"errorCode\":\"E001\"")
        assertThat(content).contains("\"timestamp\":\"2024-06-01T12:30:45.123Z\"")
    }

    @Test
    fun `should handle special characters in log fields`() {
        // given
        val filePath = tempDir.resolve("special.{pid}.jsonl").toString()
        val resolvedPath = FilePathResolver.resolve(filePath)
        val appender = FileLogAppender(filePath)
        val log = HttpLog(
            userAgent = "Agent with \"quotes\" and \\backslash",
            duration = 10,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/path/with spaces",
            httpQuery = "name=한글&value=日本語"
        )

        // when
        appender.append(log)

        // then
        val line = File(resolvedPath).readLines().first()
        val parsed = objectMapper.readTree(line)
        assertThat(parsed.get("user_agent").asText()).isEqualTo("Agent with \"quotes\" and \\backslash")
        assertThat(parsed.get("http_query").asText()).isEqualTo("name=한글&value=日本語")
    }

    @Test
    fun `should auto insert PID when no placeholder specified`() {
        // given
        val filePath = tempDir.resolve("app.jsonl").toString()
        val appender = FileLogAppender(filePath)

        // when
        appender.append(createTestLog())

        // then
        val actualFile = tempDir.resolve("app.$pid.jsonl").toFile()
        assertThat(actualFile).exists()
    }

    @Test
    fun `should use default path with PID when null path`() {
        // given
        val appender = FileLogAppender(null)

        // when
        appender.append(createTestLog())

        // then
        val defaultFile = File("/tmp/log.$pid.jsonl")
        assertThat(defaultFile).exists()

        // cleanup
        defaultFile.delete()
    }

    @Test
    fun `should create directory automatically when createDirectories is true`() {
        // given
        val nestedPath = tempDir.resolve("nested/deep/dir/log.{pid}.jsonl").toString()
        val appender = FileLogAppender(nestedPath, createDirectories = true)

        // when
        appender.append(createTestLog())

        // then
        val resolvedPath = FilePathResolver.resolve(nestedPath)
        assertThat(File(resolvedPath)).exists()
    }

    @Test
    fun `should throw exception when directory not exists and createDirectories is false`() {
        // given
        val nonExistentPath = tempDir.resolve("nonexistent/log.{pid}.jsonl").toString()

        // when & then
        assertThatThrownBy {
            FileLogAppender(nonExistentPath, createDirectories = false)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Directory does not exist")
    }

    @Test
    fun `should throw exception when directory creation fails`() {
        // given - 읽기 전용 디렉토리 생성
        val readOnlyDir = tempDir.resolve("readonly").toFile()
        readOnlyDir.mkdir()
        readOnlyDir.setWritable(false)

        val nestedPath = "${readOnlyDir.absolutePath}/nested/log.{pid}.jsonl"

        // when & then
        assertThatThrownBy {
            FileLogAppender(nestedPath, createDirectories = true)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Failed to create directory")

        // cleanup
        readOnlyDir.setWritable(true)
    }

    private fun createTestLog(
        path: String = "/test",
        statusCode: Int = 200
    ) = HttpLog(
        userAgent = "TestAgent",
        duration = 100,
        httpStatusCode = statusCode,
        httpMethod = "GET",
        httpPath = path,
        timestamp = Instant.now()
    )
}
