package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog

interface LogAppender {
    fun append(log: HttpLog)
}