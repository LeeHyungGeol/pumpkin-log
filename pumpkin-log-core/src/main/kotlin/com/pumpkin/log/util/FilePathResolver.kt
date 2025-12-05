package com.pumpkin.log.util

object FilePathResolver {

    const val DEFAULT_PATH = "/tmp/log.{pid}.jsonl"
    const val PID_PLACEHOLDER = "{pid}"

    private val currentPid: String by lazy {
        ProcessHandle.current().pid().toString()
    }

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
