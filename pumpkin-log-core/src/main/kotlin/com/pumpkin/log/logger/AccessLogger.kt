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
        val httpLog = HttpLog(
            userAgent = userAgent,
            httpMethod = method,
            httpPath = path,
            httpQuery = query,
            httpStatusCode = statusCode,
            duration = duration,
            extra = extra.filterValues { it != null }.mapValues { it.value!! }
        )
        log(httpLog)
    }
}
