package com.pumpkin.log.spring.mvc.config

import com.pumpkin.log.appender.AsyncFileLogAppender
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pumpkin.log")
data class PumpkinLogProperties(
    val enabled: Boolean = true,
    val console: ConsoleProperties = ConsoleProperties(),
    val file: FileProperties = FileProperties(),
    val excludePaths: List<String> = emptyList()
)

data class ConsoleProperties(
    val enabled: Boolean = true
)

data class FileProperties(
    val enabled: Boolean = true,
    val path: String? = null,
    val async: AsyncProperties = AsyncProperties()
)

data class AsyncProperties(
    val enabled: Boolean = false,
    val bufferSize: Int = AsyncFileLogAppender.DEFAULT_BUFFER_SIZE,
    val batchSize: Int = AsyncFileLogAppender.DEFAULT_BATCH_SIZE
)
