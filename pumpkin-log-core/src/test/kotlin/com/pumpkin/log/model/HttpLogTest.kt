package com.pumpkin.log.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class HttpLogTest {

    @Test
    fun `should have default type value`() {
        val log = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test"
        )

        assertThat(log.type).isEqualTo("log.v1.http")
    }

    @Test
    fun `should have default empty query string`() {
        val log = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test"
        )

        assertThat(log.httpQuery).isEmpty()
    }

    @Test
    fun `should have default empty extra map`() {
        val log = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test"
        )

        assertThat(log.extra).isEmpty()
    }

    @Test
    fun `should generate timestamp when not provided`() {
        val before = Instant.now()
        val log = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test"
        )
        val after = Instant.now()

        assertThat(log.timestamp).isBetween(before, after)
    }

    @Test
    fun `should use provided timestamp`() {
        val customTimestamp = Instant.parse("2024-01-15T10:30:00Z")
        val log = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test",
            timestamp = customTimestamp
        )

        assertThat(log.timestamp).isEqualTo(customTimestamp)
    }

    @Test
    fun `should support equals for same values`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val log1 = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test",
            timestamp = timestamp
        )
        val log2 = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test",
            timestamp = timestamp
        )

        assertThat(log1).isEqualTo(log2)
    }

    @Test
    fun `should not be equal for different values`() {
        val timestamp = Instant.parse("2024-01-15T10:30:00Z")
        val log1 = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test",
            timestamp = timestamp
        )
        val log2 = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 201,
            httpMethod = "GET",
            httpPath = "/test",
            timestamp = timestamp
        )

        assertThat(log1).isNotEqualTo(log2)
    }

    @Test
    fun `should support copy with modified fields`() {
        val original = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test"
        )

        val copied = original.copy(httpStatusCode = 404, httpPath = "/error")

        assertThat(copied.userAgent).isEqualTo("TestAgent")
        assertThat(copied.duration).isEqualTo(100)
        assertThat(copied.httpStatusCode).isEqualTo(404)
        assertThat(copied.httpMethod).isEqualTo("GET")
        assertThat(copied.httpPath).isEqualTo("/error")
    }

    @Test
    fun `should store extra data correctly`() {
        val extra = mapOf(
            "userId" to "user-123",
            "nested" to mapOf("key" to "value"),
            "number" to 42
        )
        val log = HttpLog(
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test",
            extra = extra
        )

        assertThat(log.extra).hasSize(3)
        assertThat(log.extra["userId"]).isEqualTo("user-123")
        assertThat(log.extra["nested"]).isEqualTo(mapOf("key" to "value"))
        assertThat(log.extra["number"]).isEqualTo(42)
    }

    @Test
    fun `should expose DEFAULT_TYPE constant`() {
        assertThat(HttpLog.DEFAULT_TYPE).isEqualTo("log.v1.http")
    }

    @Test
    fun `should allow custom type`() {
        val log = HttpLog(
            type = "custom.log.type",
            userAgent = "TestAgent",
            duration = 100,
            httpStatusCode = 200,
            httpMethod = "GET",
            httpPath = "/test"
        )

        assertThat(log.type).isEqualTo("custom.log.type")
    }
}
