package com.pumpkin.log.logger

import com.pumpkin.log.appender.LogAppender
import com.pumpkin.log.model.HttpLog

class AccessLogger(
    private val appenders: List<LogAppender>
) {

    fun log(httpLog: HttpLog) {
        appenders.forEach { it.append(httpLog) }
    }

    fun log(
        userAgent: String,
        method: String,
        path: String,
        query: String,
        statusCode: Int,
        duration: Long,
        extra: Map<String, Any?> = emptyMap()
    ) {
        HttpLog(
            userAgent = userAgent,
            httpMethod = method,
            httpPath = path,
            httpQuery = query,
            httpStatusCode = statusCode,
            duration = duration,
            extra = extra.filterNotNullValues()
        ).also { log(it) }
    }

    private fun Map<String, Any?>.filterNotNullValues(): Map<String, Any> =
        filterValues { it != null }.mapValues { it.value!! }
}
