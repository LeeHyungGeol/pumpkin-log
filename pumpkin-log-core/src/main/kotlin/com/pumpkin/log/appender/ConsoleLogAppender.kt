package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.ObjectMapperFactory

class ConsoleLogAppender : LogAppender {

    override fun append(log: HttpLog) {
        val json = ObjectMapperFactory.instance.writeValueAsString(log)
        println(json)
    }
}