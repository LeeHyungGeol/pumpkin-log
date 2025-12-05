// 순수 Kotlin 모듈 - Spring 의존성 없음

dependencies {
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)

    testImplementation(libs.bundles.test.common)
    testImplementation(libs.awaitility.kotlin)
}
