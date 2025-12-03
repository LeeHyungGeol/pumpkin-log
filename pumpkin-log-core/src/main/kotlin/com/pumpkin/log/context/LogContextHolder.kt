package com.pumpkin.log.context

object LogContextHolder {

    private val contextHolder = ThreadLocal<MutableMap<String, Any?>>()

    fun init() {
        contextHolder.set(mutableMapOf())
    }

    fun put(key: String, value: Any?) {
        val map = contextHolder.get() ?: mutableMapOf<String, Any?>().also { contextHolder.set(it) }
        map[key] = value
    }

    fun get(key: String): Any? {
        return contextHolder.get()?.get(key)
    }

    fun getAll(): Map<String, Any?> {
        return contextHolder.get()?.toMap() ?: emptyMap()
    }

    fun clear() {
        contextHolder.remove()
    }
}
