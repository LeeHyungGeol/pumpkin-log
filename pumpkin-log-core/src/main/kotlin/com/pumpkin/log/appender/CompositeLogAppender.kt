package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog

class CompositeLogAppender(
    private val appenders: List<LogAppender>
) : LogAppender {

    override fun append(log: HttpLog) {
        appenders.forEach { it.append(log) }
    }
}
