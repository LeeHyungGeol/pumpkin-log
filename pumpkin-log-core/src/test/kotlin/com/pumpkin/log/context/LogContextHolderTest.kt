package com.pumpkin.log.context

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class LogContextHolderTest {

    @AfterEach
    fun tearDown() {
        LogContextHolder.clear()
    }

    @Test
    fun `should initialize empty context`() {
        LogContextHolder.init()

        assertThat(LogContextHolder.getAll()).isEmpty()
    }

    @Test
    fun `should put and get value`() {
        LogContextHolder.init()

        LogContextHolder.put("userId", "user-123")

        assertThat(LogContextHolder.get("userId")).isEqualTo("user-123")
    }

    @Test
    fun `should return null for non-existent key`() {
        LogContextHolder.init()

        assertThat(LogContextHolder.get("nonExistent")).isNull()
    }

    @Test
    fun `should return null when get called without initialization`() {
        LogContextHolder.clear()

        assertThat(LogContextHolder.get("anyKey")).isNull()
    }

    @Test
    fun `should put multiple values`() {
        LogContextHolder.init()

        LogContextHolder.put("userId", "user-123")
        LogContextHolder.put("requestId", "req-456")
        LogContextHolder.put("traceId", "trace-789")

        assertThat(LogContextHolder.get("userId")).isEqualTo("user-123")
        assertThat(LogContextHolder.get("requestId")).isEqualTo("req-456")
        assertThat(LogContextHolder.get("traceId")).isEqualTo("trace-789")
    }

    @Test
    fun `should overwrite existing value`() {
        LogContextHolder.init()

        LogContextHolder.put("userId", "old-value")
        LogContextHolder.put("userId", "new-value")

        assertThat(LogContextHolder.get("userId")).isEqualTo("new-value")
    }

    @Test
    fun `should getAll return all values`() {
        LogContextHolder.init()

        LogContextHolder.put("key1", "value1")
        LogContextHolder.put("key2", 123)
        LogContextHolder.put("key3", true)

        val all = LogContextHolder.getAll()

        assertThat(all).hasSize(3)
        assertThat(all["key1"]).isEqualTo("value1")
        assertThat(all["key2"]).isEqualTo(123)
        assertThat(all["key3"]).isEqualTo(true)
    }

    @Test
    fun `should getAll return copy not reference`() {
        LogContextHolder.init()
        LogContextHolder.put("key", "value")

        val all = LogContextHolder.getAll()
        LogContextHolder.put("newKey", "newValue")

        assertThat(all).doesNotContainKey("newKey")
    }

    @Test
    fun `should clear remove all data`() {
        LogContextHolder.init()
        LogContextHolder.put("userId", "user-123")
        LogContextHolder.put("requestId", "req-456")

        LogContextHolder.clear()

        assertThat(LogContextHolder.getAll()).isEmpty()
        assertThat(LogContextHolder.get("userId")).isNull()
    }

    @Test
    fun `should getAll return empty map when not initialized`() {
        LogContextHolder.clear()

        assertThat(LogContextHolder.getAll()).isEmpty()
    }

    @Test
    fun `should put work without explicit init`() {
        LogContextHolder.clear()

        LogContextHolder.put("key", "value")

        assertThat(LogContextHolder.get("key")).isEqualTo("value")
    }

    @Test
    fun `should not share data between threads`() {
        val mainThreadValue = AtomicReference<Any?>()
        val otherThreadValue = AtomicReference<Any?>()
        val latch = CountDownLatch(1)

        LogContextHolder.init()
        LogContextHolder.put("userId", "main-thread-user")

        val thread = Thread {
            try {
                LogContextHolder.init()
                LogContextHolder.put("userId", "other-thread-user")
                otherThreadValue.set(LogContextHolder.get("userId"))
            } finally {
                LogContextHolder.clear()
                latch.countDown()
            }
        }
        thread.start()
        latch.await()

        mainThreadValue.set(LogContextHolder.get("userId"))

        assertThat(mainThreadValue.get()).isEqualTo("main-thread-user")
        assertThat(otherThreadValue.get()).isEqualTo("other-thread-user")
    }

    @Test
    fun `should isolate data in different threads`() {
        val otherThreadData = AtomicReference<Map<String, Any?>>()
        val latch = CountDownLatch(1)

        LogContextHolder.init()
        LogContextHolder.put("mainKey", "mainValue")

        val thread = Thread {
            try {
                otherThreadData.set(LogContextHolder.getAll())
            } finally {
                latch.countDown()
            }
        }
        thread.start()
        latch.await()

        assertThat(otherThreadData.get()).isEmpty()
        assertThat(LogContextHolder.get("mainKey")).isEqualTo("mainValue")
    }

    @Test
    fun `should support null value`() {
        LogContextHolder.init()

        LogContextHolder.put("nullKey", null)

        assertThat(LogContextHolder.getAll()).containsKey("nullKey")
        assertThat(LogContextHolder.get("nullKey")).isNull()
    }

    @Test
    fun `should support various value types`() {
        LogContextHolder.init()

        LogContextHolder.put("string", "text")
        LogContextHolder.put("int", 42)
        LogContextHolder.put("long", 123L)
        LogContextHolder.put("double", 3.14)
        LogContextHolder.put("boolean", true)
        LogContextHolder.put("list", listOf(1, 2, 3))
        LogContextHolder.put("map", mapOf("nested" to "value"))

        assertThat(LogContextHolder.get("string")).isEqualTo("text")
        assertThat(LogContextHolder.get("int")).isEqualTo(42)
        assertThat(LogContextHolder.get("long")).isEqualTo(123L)
        assertThat(LogContextHolder.get("double")).isEqualTo(3.14)
        assertThat(LogContextHolder.get("boolean")).isEqualTo(true)
        assertThat(LogContextHolder.get("list")).isEqualTo(listOf(1, 2, 3))
        assertThat(LogContextHolder.get("map")).isEqualTo(mapOf("nested" to "value"))
    }
}
