package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog

class CompositeLogAppender(
    private val appenders: List<LogAppender>,
    private val onError: ((LogAppender, Throwable) -> Unit)? = null
) : LogAppender {

    constructor(vararg appenders: LogAppender) : this(appenders.toList())

    override fun append(log: HttpLog) {
        appenders.forEach { appender ->
            runCatching { appender.append(log) }
                .onFailure { e -> onError?.invoke(appender, e)}
        }
    }
}
