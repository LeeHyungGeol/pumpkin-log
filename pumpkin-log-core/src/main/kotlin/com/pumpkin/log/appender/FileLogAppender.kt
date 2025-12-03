package com.pumpkin.log.appender

import com.pumpkin.log.model.HttpLog
import com.pumpkin.log.util.ObjectMapperFactory
import java.io.File
import java.io.FileWriter

class FileLogAppender(
    private val filePath: String = "/tmp/log.${ProcessHandle.current().pid()}.jsonl"
) : LogAppender {

    override fun append(log: HttpLog) {
        val json = ObjectMapperFactory.instance.writeValueAsString(log)
        FileWriter(File(filePath), true).use { writer ->
            writer.write(json)
            writer.write("\n")
        }
    }
}
