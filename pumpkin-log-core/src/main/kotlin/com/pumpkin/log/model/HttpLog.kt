package com.pumpkin.log.model

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

data class HttpLog(
    val type: String = DEFAULT_TYPE,
    val userAgent: String,
    val duration: Long,
    val httpStatusCode: Int,
    val httpMethod: String,
    val httpPath: String,
    val httpQuery: String = "",
    @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val extra: Map<String, Any> = emptyMap(),
    val timestamp: Instant = Instant.now()
) {
    companion object {
        const val DEFAULT_TYPE = "log.v1.http"
    }
}
