package com.pumpkin.log.model

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

/**
 * HTTP 요청 로그 데이터 클래스.
 *
 * JSON 직렬화 시 snake_case 필드명으로 변환됩니다. (예: userAgent → user_agent)
 *
 * @property type 로그 타입 식별자
 * @property userAgent 클라이언트 User-Agent 헤더 값
 * @property duration 요청 처리 시간 (밀리초)
 * @property httpStatusCode HTTP 응답 상태 코드
 * @property httpMethod HTTP 메서드 (GET, POST 등)
 * @property httpPath 요청 경로
 * @property httpQuery 쿼리 스트링 (없으면 빈 문자열)
 * @property extra 개발자가 추가하는 커스텀 데이터
 * @property timestamp 로그 생성 시각 (ISO 8601 형식으로 직렬화)
 */
data class HttpLog(
    val type: String = DEFAULT_TYPE,
    val userAgent: String,
    val duration: Long,
    val httpStatusCode: Int,
    val httpMethod: String,
    val httpPath: String,
    val httpQuery: String = "",
    @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val extra: Map<String, Any> = emptyMap(),
    val timestamp: Instant = Instant.now()
) {
    companion object {
        /** 기본 로그 타입 식별자 */
        const val DEFAULT_TYPE = "log.v1.http"
    }
}
