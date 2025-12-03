package com.pumpkin.log.spring.mvc.config

import com.pumpkin.log.appender.CompositeLogAppender
import com.pumpkin.log.appender.ConsoleLogAppender
import com.pumpkin.log.appender.FileLogAppender
import com.pumpkin.log.appender.LogAppender
import com.pumpkin.log.logger.AccessLogger
import com.pumpkin.log.spring.mvc.filter.AccessLogFilter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(PumpkinLogProperties::class)
class PumpkinLogAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "pumpkin.log.console", name = ["enabled"], havingValue = "true", matchIfMissing = true)
    fun consoleLogAppender(): ConsoleLogAppender {
        return ConsoleLogAppender()
    }

    @Bean
    @ConditionalOnProperty(prefix = "pumpkin.log.file", name = ["enabled"], havingValue = "true", matchIfMissing = true)
    fun fileLogAppender(properties: PumpkinLogProperties): FileLogAppender {
        return if (properties.file.path != null) {
            FileLogAppender(properties.file.path)
        } else {
            FileLogAppender()
        }
    }

    @Bean
    fun accessLogger(appenders: List<LogAppender>): AccessLogger {
        val compositeAppender = CompositeLogAppender(appenders)
        return AccessLogger(listOf(compositeAppender))
    }

    @Bean
    @ConditionalOnProperty(prefix = "pumpkin.log", name = ["enabled"], havingValue = "true", matchIfMissing = true)
    fun accessLogFilter(accessLogger: AccessLogger): AccessLogFilter {
        return AccessLogFilter(accessLogger)
    }
}
