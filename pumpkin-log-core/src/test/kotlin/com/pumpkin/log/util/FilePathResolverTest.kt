package com.pumpkin.log.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FilePathResolverTest {

    private val pid = ProcessHandle.current().pid().toString()

    @Test
    fun `should use default path with PID when input is null`() {
        val result = FilePathResolver.resolve(null)
        assertThat(result).isEqualTo("/tmp/log.$pid.jsonl")
    }

    @Test
    fun `should replace pid placeholder with actual PID`() {
        val result = FilePathResolver.resolve("/var/log/app.{pid}.jsonl")
        assertThat(result).isEqualTo("/var/log/app.$pid.jsonl")
    }

    @Test
    fun `should insert PID before extension when no placeholder`() {
        val result = FilePathResolver.resolve("/var/log/app.jsonl")
        assertThat(result).isEqualTo("/var/log/app.$pid.jsonl")
    }

    @Test
    fun `should append PID at end when no extension`() {
        val result = FilePathResolver.resolve("/var/log/mylog")
        assertThat(result).isEqualTo("/var/log/mylog.$pid")
    }

    @Test
    fun `should replace pid placeholder in middle of path`() {
        val result = FilePathResolver.resolve("/var/log/{pid}/app.log")
        assertThat(result).isEqualTo("/var/log/$pid/app.log")
    }
}
