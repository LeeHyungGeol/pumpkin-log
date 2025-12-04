package com.pumpkin.log.spring.mvc.config

import com.pumpkin.log.appender.AsyncFileLogAppender
import com.pumpkin.log.appender.ConsoleLogAppender
import com.pumpkin.log.appender.FileLogAppender
import com.pumpkin.log.spring.mvc.filter.AccessLogFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

class PumpkinLogAutoConfigurationTest {

    private val contextRunner = WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(PumpkinLogAutoConfiguration::class.java))

    @Test
    fun `should register FileLogAppender by default`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(FileLogAppender::class.java)
            assertThat(context).doesNotHaveBean(AsyncFileLogAppender::class.java)
        }
    }

    @Test
    fun `should register AsyncFileLogAppender when async enabled`() {
        contextRunner
            .withPropertyValues("pumpkin.log.file.async.enabled=true")
            .run { context ->
                assertThat(context).hasSingleBean(AsyncFileLogAppender::class.java)
                assertThat(context).doesNotHaveBean(FileLogAppender::class.java)
            }
    }

    @Test
    fun `should not register ConsoleLogAppender when console disabled`() {
        contextRunner
            .withPropertyValues("pumpkin.log.console.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(ConsoleLogAppender::class.java)
            }
    }

    @Test
    fun `should not register FileLogAppender when file disabled`() {
        contextRunner
            .withPropertyValues("pumpkin.log.file.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(FileLogAppender::class.java)
                assertThat(context).doesNotHaveBean(AsyncFileLogAppender::class.java)
            }
    }

    @Test
    fun `should register ConsoleLogAppender by default`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(ConsoleLogAppender::class.java)
        }
    }

    @Test
    fun `should not register AccessLogFilter when disabled`() {
        contextRunner
            .withPropertyValues("pumpkin.log.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(AccessLogFilter::class.java)
            }
    }
}
