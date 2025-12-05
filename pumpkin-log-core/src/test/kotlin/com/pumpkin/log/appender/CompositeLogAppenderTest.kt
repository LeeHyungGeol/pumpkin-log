package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import java.time.Instant

class CompositeLogAppenderTest {

    @Test
    fun `should dispatch log to all appenders`() {
        val appender1 = mockk<LogAppender>(relaxed = true)
        val appender2 = mockk<LogAppender>(relaxed = true)
        val appender3 = mockk<LogAppender>(relaxed = true)
        val composite = CompositeLogAppender(appender1, appender2, appender3)
        val log = createTestLog()

        composite.append(log)

        verify(exactly = 1) { appender1.append(log) }
        verify(exactly = 1) { appender2.append(log) }
        verify(exactly = 1) { appender3.append(log) }
    }

    @Test
    fun `should dispatch log in order`() {
        val appender1 = mockk<LogAppender>(relaxed = true)
        val appender2 = mockk<LogAppender>(relaxed = true)
        val composite = CompositeLogAppender(appender1, appender2)
        val log = createTestLog()

        composite.append(log)

        verifyOrder {
            appender1.append(log)
            appender2.append(log)
        }
    }

    @Test
    fun `should work with empty appender list`() {
        val composite = CompositeLogAppender(emptyList())
        val log = createTestLog()

        composite.append(log)
    }

    @Test
    fun `should work with single appender`() {
        val appender = mockk<LogAppender>(relaxed = true)
        val composite = CompositeLogAppender(appender)
        val log = createTestLog()

        composite.append(log)

        verify(exactly = 1) { appender.append(log) }
    }

    @Test
    fun `should dispatch multiple logs to all appenders`() {
        val appender1 = mockk<LogAppender>(relaxed = true)
        val appender2 = mockk<LogAppender>(relaxed = true)
        val composite = CompositeLogAppender(appender1, appender2)
        val log1 = createTestLog(path = "/first")
        val log2 = createTestLog(path = "/second")
        val log3 = createTestLog(path = "/third")

        composite.append(log1)
        composite.append(log2)
        composite.append(log3)

        verify(exactly = 1) { appender1.append(log1) }
        verify(exactly = 1) { appender1.append(log2) }
        verify(exactly = 1) { appender1.append(log3) }
        verify(exactly = 1) { appender2.append(log1) }
        verify(exactly = 1) { appender2.append(log2) }
        verify(exactly = 1) { appender2.append(log3) }
    }

    @Test
    fun `should accept list constructor`() {
        val appender1 = mockk<LogAppender>(relaxed = true)
        val appender2 = mockk<LogAppender>(relaxed = true)
        val composite = CompositeLogAppender(listOf(appender1, appender2))
        val log = createTestLog()

        composite.append(log)

        verify(exactly = 1) { appender1.append(log) }
        verify(exactly = 1) { appender2.append(log) }
    }

    private fun createTestLog(path: String = "/test") = HttpLog(
        userAgent = "TestAgent",
        duration = 100,
        httpStatusCode = 200,
        httpMethod = "GET",
        httpPath = path,
        timestamp = Instant.now()
    )
}
