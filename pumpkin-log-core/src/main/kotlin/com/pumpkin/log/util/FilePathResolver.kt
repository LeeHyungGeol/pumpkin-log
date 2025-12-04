package com.pumpkin.log.util

object FilePathResolver {

    private const val DEFAULT_PATH = "/tmp/log.{pid}.jsonl"
    private const val PID_PLACEHOLDER = "{pid}"

    fun resolve(path: String? = null): String {
        val basePath = path ?: DEFAULT_PATH
        val pid = ProcessHandle.current().pid().toString()

        if (basePath.contains(PID_PLACEHOLDER)) {
            return basePath.replace(PID_PLACEHOLDER, pid)
        }

        val dotIndex = basePath.lastIndexOf('.')
        return if (dotIndex > 0) {
            "${basePath.substring(0, dotIndex)}.$pid${basePath.substring(dotIndex)}"
        } else {
            "$basePath.$pid"
        }
    }
}
