package com.pumpkin.log.logger

import com.pumpkin.log.appender.LogAppender
import com.pumpkin.log.model.HttpLog

/**
 * HTTP 접근 로그를 생성하고 등록된 Appender들에 전달하는 로거.
 *
 * @param appenders 로그를 전달할 Appender 목록
 */
class AccessLogger(
    private val appenders: List<LogAppender>
) {

    /**
     * 이미 생성된 HttpLog 객체를 모든 Appender에 전달합니다.
     *
     * @param httpLog 기록할 HTTP 로그
     */
    fun log(httpLog: HttpLog) {
        appenders.forEach { it.append(httpLog) }
    }

    /**
     * HTTP 요청 정보로 HttpLog를 생성하여 모든 Appender에 전달합니다.
     *
     * @param userAgent 클라이언트 User-Agent 헤더
     * @param method HTTP 메서드
     * @param path 요청 경로
     * @param query 쿼리 스트링
     * @param statusCode HTTP 응답 상태 코드
     * @param duration 요청 처리 시간 (밀리초)
     * @param extra 추가 데이터 (null 값은 자동 제거)
     */
    fun log(
        userAgent: String,
        method: String,
        path: String,
        query: String,
        statusCode: Int,
        duration: Long,
        extra: Map<String, Any?> = emptyMap()
    ) {
        HttpLog(
            userAgent = userAgent,
            httpMethod = method,
            httpPath = path,
            httpQuery = query,
            httpStatusCode = statusCode,
            duration = duration,
            extra = extra.filterNotNullValues()
        ).also { log(it) }
    }

    private fun Map<String, Any?>.filterNotNullValues(): Map<String, Any> =
        filterValues { it != null }.mapValues { it.value!! }
}
