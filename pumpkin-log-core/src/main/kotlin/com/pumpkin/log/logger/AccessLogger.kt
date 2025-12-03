package com.pumpkin.log.logger

import com.pumpkin.log.appender.LogAppender
import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.ObjectMapperFactory
import org.slf4j.LoggerFactory

class AccessLogger(
    private val appenders: List<LogAppender>
) {

    private val logger = LoggerFactory.getLogger(AccessLogger::class.java)
    private val objectMapper = ObjectMapperFactory.instance

    fun log(httpLog: HttpLog) {
        val json = objectMapper.writeValueAsString(httpLog)

        when {
            httpLog.httpStatusCode >= 500 -> logger.error(json)
            httpLog.httpStatusCode >= 400 -> logger.warn(json)
            else -> logger.info(json)
        }

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
