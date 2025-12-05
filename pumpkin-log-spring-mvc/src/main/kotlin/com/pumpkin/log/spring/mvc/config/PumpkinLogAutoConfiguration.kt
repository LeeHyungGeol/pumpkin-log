package com.pumpkin.log.spring.mvc.config

import com.pumpkin.log.appender.AsyncFileLogAppender
import com.pumpkin.log.appender.CompositeLogAppender
import com.pumpkin.log.appender.ConsoleLogAppender
import com.pumpkin.log.appender.FileLogAppender
import com.pumpkin.log.appender.LogAppender
import com.pumpkin.log.logger.AccessLogger
import com.pumpkin.log.spring.mvc.filter.AccessLogFilter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
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
    @ConditionalOnProperty(
        prefix = "pumpkin.log.console",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun consoleLogAppender(): ConsoleLogAppender = ConsoleLogAppender()

    @Bean
    @ConditionalOnExpression(FILE_SYNC_CONDITION)
    fun fileLogAppender(properties: PumpkinLogProperties): FileLogAppender =
        FileLogAppender(properties.file.path)

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(
        prefix = "pumpkin.log.file.async",
        name = ["enabled"],
        havingValue = "true"
    )
    fun asyncFileLogAppender(properties: PumpkinLogProperties): AsyncFileLogAppender =
        properties.file.let { file ->
            AsyncFileLogAppender(
                filePath = file.path,
                bufferSize = file.async.bufferSize,
                batchSize = file.async.batchSize
            )
        }

    @Bean
    fun accessLogger(appenders: List<LogAppender>): AccessLogger =
        AccessLogger(listOf(CompositeLogAppender(appenders)))

    @Bean
    @ConditionalOnProperty(
        prefix = "pumpkin.log",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun accessLogFilter(
        accessLogger: AccessLogger,
        properties: PumpkinLogProperties
    ): AccessLogFilter = AccessLogFilter(accessLogger, properties)

    companion object {
        private const val FILE_SYNC_CONDITION =
            "\${pumpkin.log.file.enabled:true} && !\${pumpkin.log.file.async.enabled:false}"
    }
}
