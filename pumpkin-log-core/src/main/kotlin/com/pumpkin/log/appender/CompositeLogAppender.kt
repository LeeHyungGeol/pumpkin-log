package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog

class CompositeLogAppender(
    private val appenders: List<LogAppender>
) : LogAppender {

    constructor(vararg appenders: LogAppender) : this(appenders.toList())

    override fun append(log: HttpLog) {
        appenders.forEach { appender ->
            runCatching { appender.append(log) }
        }
    }
}
