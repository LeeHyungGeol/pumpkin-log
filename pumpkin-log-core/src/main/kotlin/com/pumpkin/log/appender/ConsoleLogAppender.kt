package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.ObjectMapperFactory

/**
 * 로그를 stdout에 JSON 형식으로 출력하는 Appender.
 *
 * 각 로그는 한 줄의 JSON 문자열로 출력됩니다.
 */
class ConsoleLogAppender : LogAppender {

    private val objectMapper = ObjectMapperFactory.instance

    override fun append(log: HttpLog) {
        println(objectMapper.writeValueAsString(log))
    }
}