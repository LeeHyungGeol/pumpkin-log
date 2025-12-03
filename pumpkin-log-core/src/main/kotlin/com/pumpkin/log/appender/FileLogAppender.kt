package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.ObjectMapperFactory
import java.io.File
import java.io.FileWriter

class FileLogAppender(
    private val filePath: String = "/tmp/log.${ProcessHandle.current().pid()}.jsonl"
) : LogAppender {

    private val objectMapper = ObjectMapperFactory.instance

    override fun append(log: HttpLog) {
        FileWriter(File(filePath), true).use { writer ->
            writer.appendLine(objectMapper.writeValueAsString(log))
        }
    }
}
