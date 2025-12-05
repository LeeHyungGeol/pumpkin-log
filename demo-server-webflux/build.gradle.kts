// Spring WebFlux 데모 서버

plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // SDK 모듈 의존
    implementation(project(":pumpkin-log-spring-webflux"))

    // Spring WebFlux
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.jackson.module.kotlin)

    // 테스트
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
}
