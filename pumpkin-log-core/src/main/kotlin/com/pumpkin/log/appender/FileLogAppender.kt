package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.FilePathResolver
import com.pumpkin.log.util.ObjectMapperFactory
import java.io.FileWriter

/**
 * 로그를 파일에 JSONL 형식으로 동기 기록하는 Appender.
 *
 * 각 로그는 한 줄의 JSON 문자열로 파일에 append됩니다.
 * 매 호출마다 파일을 열고 닫으므로 저트래픽 환경에 적합합니다.
 *
 * @param filePath 로그 파일 경로 (null이면 기본 경로 사용, `{pid}` 플레이스홀더 지원)
 * @see AsyncFileLogAppender 고트래픽 환경을 위한 비동기 구현
 */
class FileLogAppender(
    filePath: String? = null
) : LogAppender {

    private val resolvedFilePath: String = FilePathResolver.resolve(filePath)
    private val objectMapper = ObjectMapperFactory.instance

    override fun append(log: HttpLog) {
        FileWriter(resolvedFilePath, true).use { writer ->
            writer.appendLine(objectMapper.writeValueAsString(log))
        }
    }
}
