package com.pumpkin.demo.mvc.integration

import com.pumpkin.log.util.FilePathResolver
import com.pumpkin.log.util.ObjectMapperFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.test.web.client.TestRestTemplate
import java.io.File

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension::class)
class AccessLogIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    private val objectMapper = ObjectMapperFactory.instance
    private val logFilePath = FilePathResolver.resolve(null)

    @BeforeEach
    fun setup() {
        File(logFilePath).delete()
    }

    @Test
    fun `should output log to stdout on GET health request`(output: CapturedOutput) {
        restTemplate.getForEntity("/health", String::class.java)

        val logLine = findLogLine(output, "/health")
        assertThat(logLine).isNotNull()

        val parsed = objectMapper.readTree(logLine)
        assertThat(parsed.get("http_method").asText()).isEqualTo("GET")
        assertThat(parsed.get("http_path").asText()).isEqualTo("/health")
        assertThat(parsed.get("http_status_code").asInt()).isEqualTo(200)
    }

    @Test
    fun `should include required fields in log`(output: CapturedOutput) {
        restTemplate.postForEntity("/api/orders", null, String::class.java)

        val logLine = findLogLine(output, "/api/orders")
        assertThat(logLine).isNotNull()

        val parsed = objectMapper.readTree(logLine)
        assertThat(parsed.has("type")).isTrue()
        assertThat(parsed.has("user_agent")).isTrue()
        assertThat(parsed.has("duration")).isTrue()
        assertThat(parsed.has("http_status_code")).isTrue()
        assertThat(parsed.has("http_method")).isTrue()
        assertThat(parsed.has("http_path")).isTrue()
        assertThat(parsed.has("http_query")).isTrue()
        assertThat(parsed.has("timestamp")).isTrue()
    }

    @Test
    fun `should include userId in extra`(output: CapturedOutput) {
        restTemplate.getForEntity("/api/users/123", String::class.java)

        val logLine = findLogLine(output, "/api/users/123")
        assertThat(logLine).isNotNull()

        val parsed = objectMapper.readTree(logLine)
        assertThat(parsed.has("extra")).isTrue()
        assertThat(parsed.get("extra").get("userId").asText()).isEqualTo("123")
    }

    @Test
    fun `should write log to file`() {
        restTemplate.getForEntity("/health", String::class.java)

        val logFile = File(logFilePath)
        assertThat(logFile).exists()
        assertThat(logFile.readLines()).isNotEmpty()
    }

    @Test
    fun `should write valid JSON to file`() {
        restTemplate.postForEntity("/api/orders", null, String::class.java)

        val logFile = File(logFilePath)
        assertThat(logFile).exists()

        val lines = logFile.readLines()
        assertThat(lines).isNotEmpty()

        val foundLog = lines.find { it.contains("/api/orders") }
        assertThat(foundLog).isNotNull()

        val parsed = objectMapper.readTree(foundLog)
        assertThat(parsed.get("type").asText()).isEqualTo("log.v1.http")
    }

    @Test
    fun `should not log excluded paths`(output: CapturedOutput) {
        restTemplate.getForEntity("/actuator/health", String::class.java)

        val logLine = findLogLine(output, "/actuator/health")
        assertThat(logLine).isNull()
    }

    private fun findLogLine(output: CapturedOutput, path: String): String? {
        return output.toString().lines()
            .find { it.contains("\"type\":\"log.v1.http\"") && it.contains("\"http_path\":\"$path\"") }
    }
}
