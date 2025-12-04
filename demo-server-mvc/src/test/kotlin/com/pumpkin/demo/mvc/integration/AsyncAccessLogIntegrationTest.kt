package com.pumpkin.demo.mvc.integration

import com.pumpkin.log.util.FilePathResolver
import com.pumpkin.log.util.ObjectMapperFactory
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.test.web.client.TestRestTemplate
import java.io.File
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["pumpkin.log.file.async.enabled=true"]
)
@ExtendWith(OutputCaptureExtension::class)
class AsyncAccessLogIntegrationTest {

    companion object {
        private val TIMEOUT = Duration.ofSeconds(5)
    }

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    private val objectMapper = ObjectMapperFactory.instance
    private val logFilePath = FilePathResolver.resolve(null)

    @Test
    fun `should output log to stdout asynchronously`(output: CapturedOutput) {
        val uniqueId = UUID.randomUUID().toString()
        restTemplate.getForEntity("/api/users/$uniqueId", String::class.java)

        await().atMost(TIMEOUT).untilAsserted {
            val logLine = findLogLine(output, "/api/users/$uniqueId")
            assertThat(logLine).isNotNull()

            val parsed = objectMapper.readTree(logLine)
            assertThat(parsed.get("http_method").asText()).isEqualTo("GET")
            assertThat(parsed.get("http_path").asText()).isEqualTo("/api/users/$uniqueId")
            assertThat(parsed.get("http_status_code").asInt()).isEqualTo(200)
        }
    }

    @Test
    fun `should write log to file asynchronously`() {
        val uniqueId = UUID.randomUUID().toString()
        restTemplate.getForEntity("/api/users/$uniqueId", String::class.java)

        val logFile = File(logFilePath)

        await().atMost(TIMEOUT).untilAsserted {
            assertThat(logFile).exists()
            val lines = logFile.readLines()
            val foundLog = lines.find { it.contains("/api/users/$uniqueId") }
            assertThat(foundLog).isNotNull()

            val parsed = objectMapper.readTree(foundLog)
            assertThat(parsed.get("type").asText()).isEqualTo("log.v1.http")
        }
    }

    @Test
    fun `should include extra data asynchronously`(output: CapturedOutput) {
        val uniqueId = UUID.randomUUID().toString()
        restTemplate.getForEntity("/api/users/$uniqueId", String::class.java)

        await().atMost(TIMEOUT).untilAsserted {
            val logLine = findLogLine(output, "/api/users/$uniqueId")
            assertThat(logLine).isNotNull()

            val parsed = objectMapper.readTree(logLine)
            assertThat(parsed.has("extra")).isTrue()
            assertThat(parsed.get("extra").get("userId").asText()).isEqualTo(uniqueId)
        }
    }

    @Test
    fun `should handle concurrent requests`() {
        val requestCount = 10
        val uniqueIds = (1..requestCount).map { UUID.randomUUID().toString() }
        val executor = Executors.newFixedThreadPool(requestCount)

        uniqueIds.forEach { id ->
            executor.submit {
                restTemplate.getForEntity("/api/users/$id", String::class.java)
            }
        }

        executor.shutdown()
        val terminated = executor.awaitTermination(10, TimeUnit.SECONDS)
        assertThat(terminated)
            .withFailMessage("Tasks did not finish within timeout")
            .isTrue()

        val logFile = File(logFilePath)

        await().atMost(TIMEOUT).untilAsserted {
            assertThat(logFile).exists()
            val lines = logFile.readLines()

            uniqueIds.forEach { id ->
                val foundLog = lines.find { it.contains("/api/users/$id") }
                assertThat(foundLog)
                    .withFailMessage("Log for user $id not found")
                    .isNotNull()
            }
        }
    }

    private fun findLogLine(output: CapturedOutput, path: String): String? {
        return output.toString().lines()
            .find { it.contains("\"type\":\"log.v1.http\"") && it.contains("\"http_path\":\"$path\"") }
    }
}
