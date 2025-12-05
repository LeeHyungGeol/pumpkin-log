package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog

/**
 * 여러 Appender를 조합하여 하나의 Appender처럼 사용하는 컴포지트 패턴 구현.
 *
 * 로그가 append되면 등록된 모든 Appender에 순차적으로 전달합니다.
 *
 * @param appenders 조합할 Appender 목록
 */
class CompositeLogAppender(
    private val appenders: List<LogAppender>
) : LogAppender {

    /**
     * vararg를 사용한 편의 생성자.
     *
     * @param appenders 조합할 Appender들
     */
    constructor(vararg appenders: LogAppender) : this(appenders.toList())

    override fun append(log: HttpLog) {
        appenders.forEach { it.append(log) }
    }
}
