package com.pumpkin.log.context

/**
 * ThreadLocal 기반의 로그 컨텍스트 저장소.
 *
 * HTTP 요청 처리 중 개발자가 추가하고 싶은 데이터를 저장하고,
 * 요청 완료 시 로그의 `extra` 필드로 출력됩니다.
 *
 * 사용 예시:
 * ```kotlin
 * LogContextHolder.init()
 * try {
 *     LogContextHolder.put("userId", "123")
 *     LogContextHolder.put("action", "purchase")
 *     // 비즈니스 로직 처리
 * } finally {
 *     val extra = LogContextHolder.getAll()
 *     // 로그 기록
 *     LogContextHolder.clear()
 * }
 * ```
 *
 * **주의**: 스레드 풀 재사용 시 데이터 오염을 방지하기 위해 반드시 [clear]를 호출해야 합니다.
 */
object LogContextHolder {

    private val contextHolder = ThreadLocal<MutableMap<String, Any?>>()

    /**
     * 현재 스레드의 컨텍스트를 초기화합니다.
     * 요청 처리 시작 시 호출합니다.
     */
    fun init() {
        contextHolder.set(mutableMapOf())
    }

    /**
     * 컨텍스트에 데이터를 추가합니다.
     *
     * @param key 데이터 키
     * @param value 데이터 값 (null 가능)
     */
    fun put(key: String, value: Any?) {
        val map = contextHolder.get()
            ?: mutableMapOf<String, Any?>().also { contextHolder.set(it) }
        map[key] = value
    }

    /**
     * 컨텍스트에서 데이터를 조회합니다.
     *
     * @param key 데이터 키
     * @return 저장된 값 또는 null
     */
    fun get(key: String): Any? = contextHolder.get()?.get(key)

    /**
     * 컨텍스트의 모든 데이터를 복사본으로 반환합니다.
     *
     * @return 저장된 모든 데이터의 불변 맵
     */
    fun getAll(): Map<String, Any?> = contextHolder.get()?.toMap() ?: emptyMap()

    /**
     * 현재 스레드의 컨텍스트를 제거합니다.
     * 요청 처리 완료 시 반드시 호출해야 합니다.
     */
    fun clear() {
        contextHolder.remove()
    }
}
