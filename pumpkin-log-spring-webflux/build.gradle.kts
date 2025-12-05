// Spring WebFlux 통합 모듈

plugins {
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    // 코어 모듈 의존
    implementation(project(":pumpkin-log-core"))

    // Spring WebFlux (컴파일 시에만)
    compileOnly(libs.spring.boot.starter.webflux.versioned)
    compileOnly(libs.spring.boot.autoconfigure.versioned)

    // 테스트
    testImplementation(libs.spring.boot.starter.test.versioned)
    testImplementation(libs.spring.boot.starter.webflux.versioned)
    testImplementation(libs.reactor.test)
    testImplementation(libs.bundles.test.common)
}
