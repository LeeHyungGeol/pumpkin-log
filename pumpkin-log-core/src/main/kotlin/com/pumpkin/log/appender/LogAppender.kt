package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog

/**
 * 로그 출력 대상을 추상화하는 인터페이스.
 *
 * 콘솔, 파일, 외부 시스템 등 다양한 출력 대상에 대한 구현체를 제공합니다.
 *
 * @see ConsoleLogAppender stdout 출력 구현
 * @see FileLogAppender 파일 출력 구현 (동기)
 * @see AsyncFileLogAppender 파일 출력 구현 (비동기)
 * @see CompositeLogAppender 여러 Appender 조합
 */
interface LogAppender {
    /**
     * 로그를 출력 대상에 기록합니다.
     *
     * @param log 기록할 HTTP 로그 데이터
     */
    fun append(log: HttpLog)
}