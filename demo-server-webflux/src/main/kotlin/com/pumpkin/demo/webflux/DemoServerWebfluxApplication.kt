package com.pumpkin.demo.webflux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DemoServerWebfluxApplication

fun main(args: Array<String>) {
    runApplication<DemoServerWebfluxApplication>(*args)
}
