package com.pumpkin.log.logger

import com.pumpkin.log.appender.LogAppender
import com.pumpkin.log.model.HttpLog
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant

class AccessLoggerTest {

    @Test
    fun `should call append on all appenders`() {
        val mockAppender1 = mockk<LogAppender>(relaxed = true)
        val mockAppender2 = mockk<LogAppender>(relaxed = true)
        val accessLogger = AccessLogger(listOf(mockAppender1, mockAppender2))
        val httpLog = createTestLog(statusCode = 200)

        accessLogger.log(httpLog)

        verify(exactly = 1) { mockAppender1.append(httpLog) }
        verify(exactly = 1) { mockAppender2.append(httpLog) }
    }

    @Test
    fun `should handle empty appenders list`() {
        val accessLogger = AccessLogger(emptyList())
        val httpLog = createTestLog(statusCode = 200)

        assertThatCode { accessLogger.log(httpLog) }.doesNotThrowAnyException()
    }

    @ParameterizedTest
    @ValueSource(ints = [200, 201, 301, 400, 404, 500, 503])
    fun `should pass httpLog to appender for various status codes`(statusCode: Int) {
        val mockAppender = mockk<LogAppender>(relaxed = true)
        val accessLogger = AccessLogger(listOf(mockAppender))
        val httpLog = createTestLog(statusCode = statusCode)

        accessLogger.log(httpLog)

        verify { mockAppender.append(match { it.httpStatusCode == statusCode }) }
    }

    @Test
    fun `should log with parameters and call appenders`() {
        val mockAppender = mockk<LogAppender>(relaxed = true)
        val accessLogger = AccessLogger(listOf(mockAppender))

        accessLogger.log(
            userAgent = "TestAgent",
            method = "POST",
            path = "/api/users",
            query = "name=test",
            statusCode = 201,
            duration = 150,
            extra = mapOf("userId" to "123")
        )

        verify(exactly = 1) {
            mockAppender.append(match { log ->
                log.userAgent == "TestAgent" &&
                log.httpMethod == "POST" &&
                log.httpPath == "/api/users" &&
                log.httpQuery == "name=test" &&
                log.httpStatusCode == 201 &&
                log.duration == 150L &&
                log.extra["userId"] == "123"
            })
        }
    }

    @Test
    fun `should filter null values from extra map`() {
        val mockAppender = mockk<LogAppender>(relaxed = true)
        val accessLogger = AccessLogger(listOf(mockAppender))

        accessLogger.log(
            userAgent = "TestAgent",
            method = "GET",
            path = "/test",
            query = "",
            statusCode = 200,
            duration = 100,
            extra = mapOf("key1" to "value1", "key2" to null)
        )

        verify(exactly = 1) {
            mockAppender.append(match { log ->
                log.extra.containsKey("key1") && !log.extra.containsKey("key2")
            })
        }
    }

    @Test
    fun `should create HttpLog with correct type`() {
        val capturedLogs = mutableListOf<HttpLog>()
        val capturingAppender = object : LogAppender {
            override fun append(log: HttpLog) {
                capturedLogs.add(log)
            }
        }
        val accessLogger = AccessLogger(listOf(capturingAppender))

        accessLogger.log(createTestLog(statusCode = 200))

        assertThat(capturedLogs).hasSize(1)
        assertThat(capturedLogs[0].type).isEqualTo("log.v1.http")
    }

    @Test
    fun `should preserve all fields in HttpLog`() {
        val capturedLogs = mutableListOf<HttpLog>()
        val capturingAppender = object : LogAppender {
            override fun append(log: HttpLog) {
                capturedLogs.add(log)
            }
        }
        val accessLogger = AccessLogger(listOf(capturingAppender))
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val httpLog = HttpLog(
            userAgent = "Mozilla/5.0",
            duration = 250,
            httpStatusCode = 404,
            httpMethod = "DELETE",
            httpPath = "/api/resource/123",
            httpQuery = "force=true",
            extra = mapOf("requestId" to "req-123"),
            timestamp = timestamp
        )

        accessLogger.log(httpLog)

        assertThat(capturedLogs).hasSize(1)
        val captured = capturedLogs[0]
        assertThat(captured.userAgent).isEqualTo("Mozilla/5.0")
        assertThat(captured.duration).isEqualTo(250)
        assertThat(captured.httpStatusCode).isEqualTo(404)
        assertThat(captured.httpMethod).isEqualTo("DELETE")
        assertThat(captured.httpPath).isEqualTo("/api/resource/123")
        assertThat(captured.httpQuery).isEqualTo("force=true")
        assertThat(captured.extra).containsEntry("requestId", "req-123")
        assertThat(captured.timestamp).isEqualTo(timestamp)
    }

    private fun createTestLog(statusCode: Int) = HttpLog(
        userAgent = "TestAgent",
        duration = 100,
        httpStatusCode = statusCode,
        httpMethod = "GET",
        httpPath = "/test",
        httpQuery = "",
        timestamp = Instant.now()
    )
}
