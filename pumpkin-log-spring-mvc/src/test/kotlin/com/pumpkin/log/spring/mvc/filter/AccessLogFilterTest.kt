package com.pumpkin.log.spring.mvc.filter

import com.pumpkin.log.context.LogContextHolder
import com.pumpkin.log.logger.AccessLogger
import com.pumpkin.log.spring.mvc.config.PumpkinLogProperties
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AccessLogFilterTest {

    private lateinit var accessLogger: AccessLogger
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        accessLogger = mockk(relaxed = true)
        filterChain = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        LogContextHolder.clear()
    }

    @Test
    fun `should pass all correct parameters to AccessLogger`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("POST", "/api/users").apply {
            queryString = "type=admin"
            addHeader("User-Agent", "TestBrowser/1.0")
        }
        val response = MockHttpServletResponse().apply {
            status = 201
        }
        val filterChainWithContext = FilterChain { _, _ ->
            LogContextHolder.put("traceId", "trace-999")
        }

        filter.doFilter(request, response, filterChainWithContext)

        verify(exactly = 1) {
            accessLogger.log(
                userAgent = "TestBrowser/1.0",
                method = "POST",
                path = "/api/users",
                query = "type=admin",
                statusCode = 201,
                duration = match { it >= 0 },
                extra = match { it["traceId"] == "trace-999" }
            )
        }
    }

    @Test
    fun `should measure duration correctly`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/test")
        val response = MockHttpServletResponse()
        val slowFilterChain = FilterChain { _, _ ->
            Thread.sleep(50)
        }

        filter.doFilter(request, response, slowFilterChain)

        verify {
            accessLogger.log(
                userAgent = any(),
                method = any(),
                path = any(),
                query = any(),
                statusCode = any(),
                duration = match { it >= 50 },
                extra = any()
            )
        }
    }

    @Test
    fun `should pass correct userAgent to AccessLogger`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/test").apply {
            addHeader("User-Agent", "Mozilla/5.0 TestBrowser")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify {
            accessLogger.log(
                userAgent = "Mozilla/5.0 TestBrowser",
                method = any(),
                path = any(),
                query = any(),
                statusCode = any(),
                duration = any(),
                extra = any()
            )
        }
    }

    @Test
    fun `should pass empty string when User-Agent header is absent`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/test")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify {
            accessLogger.log(
                userAgent = "",
                method = any(),
                path = any(),
                query = any(),
                statusCode = any(),
                duration = any(),
                extra = any()
            )
        }
    }

    @Test
    fun `should pass correct method to AccessLogger`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("POST", "/api/users")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify {
            accessLogger.log(
                userAgent = any(),
                method = "POST",
                path = any(),
                query = any(),
                statusCode = any(),
                duration = any(),
                extra = any()
            )
        }
    }

    @Test
    fun `should pass correct path to AccessLogger`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/api/orders/123")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify {
            accessLogger.log(
                userAgent = any(),
                method = any(),
                path = "/api/orders/123",
                query = any(),
                statusCode = any(),
                duration = any(),
                extra = any()
            )
        }
    }

    @Test
    fun `should pass correct query string to AccessLogger`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/search").apply {
            queryString = "q=test&page=1"
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify {
            accessLogger.log(
                userAgent = any(),
                method = any(),
                path = any(),
                query = "q=test&page=1",
                statusCode = any(),
                duration = any(),
                extra = any()
            )
        }
    }

    @Test
    fun `should pass empty string when query string is null`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/test")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify {
            accessLogger.log(
                userAgent = any(),
                method = any(),
                path = any(),
                query = "",
                statusCode = any(),
                duration = any(),
                extra = any()
            )
        }
    }

    @Test
    fun `should pass correct statusCode to AccessLogger`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/test")
        val response = MockHttpServletResponse().apply {
            status = 201
        }

        filter.doFilter(request, response, filterChain)

        verify {
            accessLogger.log(
                userAgent = any(),
                method = any(),
                path = any(),
                query = any(),
                statusCode = 201,
                duration = any(),
                extra = any()
            )
        }
    }

    @Test
    fun `should not log for excluded paths`() {
        val properties = PumpkinLogProperties(excludePaths = listOf("/health", "/metrics"))
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/health")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify(exactly = 1) { filterChain.doFilter(request, response) }
        verify(exactly = 0) {
            accessLogger.log(
                userAgent = any(),
                method = any(),
                path = any(),
                query = any(),
                statusCode = any(),
                duration = any(),
                extra = any()
            )
        }
    }

    @Test
    fun `should not log for excluded paths with wildcard pattern`() {
        val properties = PumpkinLogProperties(excludePaths = listOf("/actuator/**"))
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/actuator/health")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify(exactly = 1) { filterChain.doFilter(request, response) }
        verify(exactly = 0) {
            accessLogger.log(
                userAgent = any(),
                method = any(),
                path = any(),
                query = any(),
                statusCode = any(),
                duration = any(),
                extra = any()
            )
        }
    }

    @Test
    fun `should log for non-excluded paths`() {
        val properties = PumpkinLogProperties(excludePaths = listOf("/health", "/metrics"))
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/api/users")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify(exactly = 1) {
            accessLogger.log(
                userAgent = any(),
                method = any(),
                path = "/api/users",
                query = any(),
                statusCode = any(),
                duration = any(),
                extra = any()
            )
        }
    }

    @Test
    fun `should clear LogContextHolder after request`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/test")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertThat(LogContextHolder.getAll()).isEmpty()
    }

    @Test
    fun `should clear LogContextHolder even when exception occurs`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/test")
        val response = MockHttpServletResponse()
        val throwingFilterChain = FilterChain { _, _ ->
            LogContextHolder.put("key", "value")
            throw RuntimeException("Test exception")
        }

        assertThatThrownBy {
            filter.doFilter(request, response, throwingFilterChain)
        }.isInstanceOf(RuntimeException::class.java)

        assertThat(LogContextHolder.getAll()).isEmpty()
    }

    @Test
    fun `should pass extra data from LogContextHolder to AccessLogger`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/test")
        val response = MockHttpServletResponse()
        val filterChainWithContext = FilterChain { _, _ ->
            LogContextHolder.put("userId", "user-123")
            LogContextHolder.put("requestId", "req-456")
        }

        filter.doFilter(request, response, filterChainWithContext)

        verify {
            accessLogger.log(
                userAgent = any(),
                method = any(),
                path = any(),
                query = any(),
                statusCode = any(),
                duration = any(),
                extra = match { extra ->
                    extra["userId"] == "user-123" && extra["requestId"] == "req-456"
                }
            )
        }
    }

    @Test
    fun `should call AccessLogger log even when filterChain throws exception`() {
        val properties = PumpkinLogProperties()
        val filter = AccessLogFilter(accessLogger, properties)
        val request = MockHttpServletRequest("GET", "/test")
        val response = MockHttpServletResponse()
        val throwingFilterChain = FilterChain { _, _ ->
            throw RuntimeException("Test exception")
        }

        assertThatThrownBy {
            filter.doFilter(request, response, throwingFilterChain)
        }.isInstanceOf(RuntimeException::class.java)

        verify(exactly = 1) {
            accessLogger.log(
                userAgent = any(),
                method = any(),
                path = any(),
                query = any(),
                statusCode = any(),
                duration = any(),
                extra = any()
            )
        }
    }
}
