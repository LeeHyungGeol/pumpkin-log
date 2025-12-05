package com.pumpkin.demo.mvc.controller

import com.pumpkin.log.context.LogContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Pumpkin Log SDK 데모용 샘플 컨트롤러.
 *
 * [LogContextHolder]를 사용하여 로그에 커스텀 데이터를 추가하는 예시를 보여줍니다.
 */
@RestController
class SampleController {

    /**
     * 헬스체크 엔드포인트.
     */
    @GetMapping("/health")
    fun health(): String = "OK"

    /**
     * 사용자 조회 엔드포인트.
     *
     * 로그에 userId를 extra 데이터로 추가합니다.
     */
    @GetMapping("/api/users/{id}")
    fun getUser(@PathVariable id: String): Map<String, String> {
        LogContextHolder.put("userId", id)
        return mapOf("id" to id, "name" to "User $id")
    }

    /**
     * 주문 생성 엔드포인트.
     *
     * 로그에 orderId를 extra 데이터로 추가합니다.
     */
    @PostMapping("/api/orders")
    fun createOrder(): Map<String, String> {
        val orderId = UUID.randomUUID().toString()
        LogContextHolder.put("orderId", orderId)
        return mapOf("orderId" to orderId, "status" to "CREATED")
    }
}
