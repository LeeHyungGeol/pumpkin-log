package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.FilePathResolver
import com.pumpkin.log.util.ObjectMapperFactory
import java.io.FileWriter

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
