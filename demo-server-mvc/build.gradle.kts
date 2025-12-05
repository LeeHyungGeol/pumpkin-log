// Spring WebMVC 데모 서버

plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // SDK 모듈 의존
    implementation(project(":pumpkin-log-spring-mvc"))

    // Spring Web
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.module.kotlin)

    // 테스트
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.awaitility.kotlin)
}
