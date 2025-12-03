plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    // 코어 모듈 의존
    implementation(project(":pumpkin-log-core"))

    // Spring WebFlux (컴파일 시에만)
    compileOnly("org.springframework.boot:spring-boot-starter-webflux:3.4.12")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.4.12")

    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.12")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux:3.4.12")
    testImplementation("io.projectreactor:reactor-test:3.6.4")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.assertj:assertj-core:3.25.3")
}
