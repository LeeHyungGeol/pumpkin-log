package com.pumpkin.log.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * 로그 직렬화를 위한 공통 Jackson ObjectMapper 팩토리.
 *
 * 설정:
 * - snake_case 필드 네이밍 (예: userAgent → user_agent)
 * - ISO 8601 형식 날짜/시간 직렬화
 * - Kotlin 데이터 클래스 지원
 */
object ObjectMapperFactory {

    /**
     * 로그 직렬화용으로 설정된 ObjectMapper 싱글톤 인스턴스.
     */
    val instance: ObjectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}