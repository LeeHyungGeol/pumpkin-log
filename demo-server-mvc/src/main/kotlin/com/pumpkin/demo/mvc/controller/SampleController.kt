package com.pumpkin.demo.mvc.controller

import com.pumpkin.log.context.LogContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class SampleController {

    @GetMapping("/health")
    fun health(): String {
        return "OK"
    }

    @GetMapping("/api/users/{id}")
    fun getUser(@PathVariable id: String): Map<String, String> {
        LogContextHolder.put("userId", id)
        return mapOf("id" to id, "name" to "User $id")
    }

    @PostMapping("/api/orders")
    fun createOrder(): Map<String, String> {
        val orderId = UUID.randomUUID().toString()
        LogContextHolder.put("orderId", orderId)
        return mapOf("orderId" to orderId, "status" to "CREATED")
    }
}
