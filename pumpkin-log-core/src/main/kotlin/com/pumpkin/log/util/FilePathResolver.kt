package com.pumpkin.log.util

/**
 * 로그 파일 경로에 PID를 자동으로 포함시키는 유틸리티.
 *
 * 여러 프로세스가 동시에 실행될 때 로그 파일 충돌을 방지합니다.
 */
object FilePathResolver {

    /** 기본 로그 파일 경로 */
    const val DEFAULT_PATH = "/tmp/log.{pid}.jsonl"

    /** PID 플레이스홀더 */
    const val PID_PLACEHOLDER = "{pid}"

    private val currentPid: String by lazy {
        ProcessHandle.current().pid().toString()
    }

    /**
     * 파일 경로를 실제 PID가 포함된 경로로 변환합니다.
     *
     * - `{pid}` 플레이스홀더가 있으면 실제 PID로 치환
     * - 플레이스홀더가 없으면 확장자 앞에 PID 자동 삽입
     * - 경로가 null이면 기본 경로 사용
     *
     * @param path 원본 파일 경로 (null 가능)
     * @return PID가 포함된 파일 경로
     *
     * 사용 예시:
     * ```kotlin
     * FilePathResolver.resolve("/var/log/app.{pid}.jsonl")  // → /var/log/app.12345.jsonl
     * FilePathResolver.resolve("/var/log/app.jsonl")        // → /var/log/app.12345.jsonl
     * FilePathResolver.resolve(null)                        // → /tmp/log.12345.jsonl
     * ```
     */
    fun resolve(path: String? = null): String {
        val basePath = path ?: DEFAULT_PATH

        if (basePath.contains(PID_PLACEHOLDER)) {
            return basePath.replace(PID_PLACEHOLDER, currentPid)
        }

        return insertPidBeforeExtension(basePath)
    }

    private fun insertPidBeforeExtension(path: String): String {
        val dotIndex = path.lastIndexOf('.')
        return if (dotIndex > 0) {
            "${path.substring(0, dotIndex)}.$currentPid${path.substring(dotIndex)}"
        } else {
            "$path.$currentPid"
        }
    }
}
