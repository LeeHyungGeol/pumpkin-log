package com.pumpkin.log.spring.mvc.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = "pumpkin.log")
data class PumpkinLogProperties(
    val enabled: Boolean = true,
    @NestedConfigurationProperty
    val console: ConsoleProperties = ConsoleProperties(),
    @NestedConfigurationProperty
    val file: FileProperties = FileProperties()
)

data class ConsoleProperties(
    val enabled: Boolean = true
)

data class FileProperties(
    val enabled: Boolean = true,
    val path: String? = null
)
