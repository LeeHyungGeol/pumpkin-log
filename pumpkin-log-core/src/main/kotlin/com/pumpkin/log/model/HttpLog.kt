package com.pumpkin.log.model

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

data class HttpLog(
    val type: String = "log.v1.http",
    val userAgent: String,
    val duration: Long,
    val httpStatusCode: Int,
    val httpMethod: String,
    val httpPath: String,
    val httpQuery: String = "",
    @field:JsonInclude(JsonInclude.Include.NON_EMPTY)  // 빈 맵이면 JSON에서 생략
    val extra: Map<String, Any> = emptyMap(),
    val timestamp: Instant = Instant.now()
)
