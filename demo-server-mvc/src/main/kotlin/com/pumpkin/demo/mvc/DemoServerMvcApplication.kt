package com.pumpkin.demo.mvc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DemoServerMvcApplication

fun main(args: Array<String>) {
    runApplication<DemoServerMvcApplication>(*args)
}
