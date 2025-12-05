package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.ObjectMapperFactory

class ConsoleLogAppender : LogAppender {

    private val objectMapper = ObjectMapperFactory.instance

    override fun append(log: HttpLog) {
        println(objectMapper.writeValueAsString(log))
    }
}
