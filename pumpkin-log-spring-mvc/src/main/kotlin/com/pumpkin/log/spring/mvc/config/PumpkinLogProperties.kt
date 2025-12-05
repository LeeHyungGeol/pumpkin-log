package com.pumpkin.log.spring.mvc.config

import com.pumpkin.log.appender.AsyncFileLogAppender
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Pumpkin Log SDK 설정 프로퍼티.
 *
 * `application.yml`에서 `pumpkin.log.*` 프로퍼티로 설정합니다.
 *
 * @property enabled SDK 전체 활성화 여부
 * @property console 콘솔 출력 설정
 * @property file 파일 출력 설정
 * @property excludePaths 로그 제외 경로 패턴 목록 (Ant 스타일)
 */
@ConfigurationProperties(prefix = "pumpkin.log")
data class PumpkinLogProperties(
    val enabled: Boolean = true,
    val console: ConsoleProperties = ConsoleProperties(),
    val file: FileProperties = FileProperties(),
    val excludePaths: List<String> = emptyList()
)

/**
 * 콘솔 출력 설정.
 *
 * @property enabled stdout 로그 출력 활성화 여부
 */
data class ConsoleProperties(
    val enabled: Boolean = true
)

/**
 * 파일 출력 설정.
 *
 * @property enabled 파일 로그 출력 활성화 여부
 * @property path 로그 파일 경로 (`{pid}` 플레이스홀더 지원)
 * @property async 비동기 모드 설정
 */
data class FileProperties(
    val enabled: Boolean = true,
    val path: String? = null,
    val async: AsyncProperties = AsyncProperties()
)

/**
 * 비동기 파일 로깅 설정.
 *
 * @property enabled 비동기 모드 활성화 여부
 * @property bufferSize 큐 버퍼 크기
 * @property batchSize 배치 쓰기 크기
 */
data class AsyncProperties(
    val enabled: Boolean = false,
    val bufferSize: Int = AsyncFileLogAppender.DEFAULT_BUFFER_SIZE,
    val batchSize: Int = AsyncFileLogAppender.DEFAULT_BATCH_SIZE
)
