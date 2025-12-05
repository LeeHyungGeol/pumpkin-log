// Spring WebMVC 통합 모듈

plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.kapt)
}

dependencies {
    // 코어 모듈 의존 (api로 전이 의존성 노출)
    api(project(":pumpkin-log-core"))

    // Spring Web (컴파일 시에만, 런타임은 사용자가 제공)
    compileOnly(libs.spring.boot.starter.web.versioned)
    compileOnly(libs.spring.boot.autoconfigure.versioned)

    // Configuration Processor
    kapt(libs.spring.boot.configuration.processor.versioned)

    // 테스트
    testImplementation(libs.spring.boot.starter.test.versioned)
    testImplementation(libs.spring.boot.starter.web.versioned)
    testImplementation(libs.bundles.test.common)
}
