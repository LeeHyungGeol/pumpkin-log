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

/**
 * Pumpkin Log SDK의 Spring Boot AutoConfiguration.
 *
 * 설정에 따라 Appender Bean들을 조건부로 등록합니다:
 * - `pumpkin.log.console.enabled=true` → [ConsoleLogAppender]
 * - `pumpkin.log.file.enabled=true && async.enabled=false` → [FileLogAppender]
 * - `pumpkin.log.file.async.enabled=true` → [AsyncFileLogAppender]
 */
@Configuration
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(PumpkinLogProperties::class)
class PumpkinLogAutoConfiguration {

    /**
     * 콘솔 로그 Appender Bean.
     * `pumpkin.log.console.enabled=true` (기본값) 일 때 등록됩니다.
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "pumpkin.log.console",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun consoleLogAppender(): ConsoleLogAppender = ConsoleLogAppender()

    /**
     * 동기 파일 로그 Appender Bean.
     * `pumpkin.log.file.enabled=true && async.enabled=false` 일 때 등록됩니다.
     */
    @Bean
    @ConditionalOnExpression(FILE_SYNC_CONDITION)
    fun fileLogAppender(properties: PumpkinLogProperties): FileLogAppender =
        FileLogAppender(properties.file.path)

    /**
     * 비동기 파일 로그 Appender Bean.
     * `pumpkin.log.file.async.enabled=true` 일 때 등록됩니다.
     */
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

    /**
     * 모든 Appender를 조합한 AccessLogger Bean.
     */
    @Bean
    fun accessLogger(appenders: List<LogAppender>): AccessLogger =
        AccessLogger(listOf(CompositeLogAppender(appenders)))

    /**
     * HTTP 요청 로깅 필터 Bean.
     * `pumpkin.log.enabled=true` (기본값) 일 때 등록됩니다.
     */
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
