package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.ObjectMapperFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Instant

class ConsoleLogAppenderTest {

    private val objectMapper = ObjectMapperFactory.instance
    private val originalOut = System.out
    private lateinit var outputStream: ByteArrayOutputStream

    @BeforeEach
    fun setUp() {
        outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
    }

    @AfterEach
    fun tearDown() {
        System.setOut(originalOut)
    }

    @Test
    fun `should output log in JSON format to stdout`() {
        val appender = ConsoleLogAppender()
        val log = createTestLog()

        appender.append(log)

        val output = outputStream.toString().trim()
        val parsed = objectMapper.readTree(output)
        assertThat(parsed.has("type")).isTrue()
        assertThat(parsed.get("type").asText()).isEqualTo("log.v1.http")
    }

    @Test
    fun `should output fields in snake_case`() {
        val appender = ConsoleLogAppender()
        val log = createTestLog()

        appender.append(log)

        val output = outputStream.toString()
        assertThat(output).contains("\"user_agent\"")
        assertThat(output).contains("\"http_status_code\"")
        assertThat(output).contains("\"http_method\"")
        assertThat(output).contains("\"http_path\"")
        assertThat(output).contains("\"http_query\"")
        assertThat(output).doesNotContain("\"userAgent\"")
        assertThat(output).doesNotContain("\"httpStatusCode\"")
    }

    @Test
    fun `should output timestamp in ISO 8601 format`() {
        val appender = ConsoleLogAppender()
        val log = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test",
            timestamp = Instant.parse("2024-01-15T10:30:45.123Z")
        )

        appender.append(log)

        val output = outputStream.toString()
        assertThat(output).contains("\"timestamp\":\"2024-01-15T10:30:45.123Z\"")
    }

    @Test
    fun `should serialize extra field correctly`() {
        val appender = ConsoleLogAppender()
        val log = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test",
            extra = mapOf(
                "userId" to "12345",
                "nested" to mapOf("key" to "value")
            )
        )

        appender.append(log)

        val output = outputStream.toString()
        val parsed = objectMapper.readTree(output)
        assertThat(parsed.has("extra")).isTrue()
        assertThat(parsed.get("extra").get("userId").asText()).isEqualTo("12345")
    }

    @Test
    fun `should not include extra field when empty`() {
        val appender = ConsoleLogAppender()
        val log = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test",
            extra = emptyMap()
        )

        appender.append(log)

        val output = outputStream.toString()
        val parsed = objectMapper.readTree(output)
        assertThat(parsed.has("extra")).isFalse()
        assertThat(output).doesNotContain("\"extra\"")
    }

    @Test
    fun `should not include extra field by default`() {
        val appender = ConsoleLogAppender()
        val log = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test"
        )

        appender.append(log)

        val output = outputStream.toString()
        assertThat(output).doesNotContain("\"extra\"")
    }

    private fun createTestLog() = HttpLog(
        userAgent = "TestAgent",
        duration = 100,
        httpStatusCode = 200,
        httpMethod = "GET",
        httpPath = "/test",
        timestamp = Instant.now()
    )
}
