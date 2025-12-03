package com.pumpkin.log.spring.mvc.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = "pumpkin.log")
data class PumpkinLogProperties(
    val enabled: Boolean = true,
    @NestedConfigurationProperty
    val console: ConsoleProperties = ConsoleProperties(),
    @NestedConfigurationProperty
    val file: FileProperties = FileProperties(),
    val excludePaths: List<String> = emptyList(),
    @NestedConfigurationProperty
    val level: LevelProperties = LevelProperties()
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
    val bufferSize: Int = 1000,
    val batchSize: Int = 100
)

data class LevelProperties(
    val default: String = "INFO",
    val client4xx: String = "WARN",
    val server5xx: String = "ERROR"
)
