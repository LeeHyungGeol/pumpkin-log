package com.pumpkin.log.spring.mvc.filter

import com.pumpkin.log.context.LogContextHolder
import com.pumpkin.log.logger.AccessLogger
import com.pumpkin.log.spring.mvc.config.PumpkinLogProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

/**
 * HTTP 요청을 가로채서 접근 로그를 기록하는 서블릿 필터.
 *
 * [OncePerRequestFilter]를 상속하여 forward/include 시 중복 실행을 방지합니다.
 *
 * @param accessLogger 로그를 기록할 AccessLogger
 * @param properties SDK 설정 (경로 제외 패턴 등)
 */
class AccessLogFilter(
    private val accessLogger: AccessLogger,
    private val properties: PumpkinLogProperties
) : OncePerRequestFilter() {

    private val pathMatcher = AntPathMatcher()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (shouldExclude(request.requestURI)) {
            filterChain.doFilter(request, response)
            return
        }

        LogContextHolder.init()
        val startTime = System.currentTimeMillis()

        try {
            filterChain.doFilter(request, response)
        } finally {
            logRequest(request, response, startTime)
            LogContextHolder.clear()
        }
    }

    private fun logRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        startTime: Long
    ) {
        accessLogger.log(
            userAgent = request.getHeader(USER_AGENT_HEADER).orEmpty(),
            method = request.method,
            path = request.requestURI,
            query = request.queryString.orEmpty(),
            statusCode = response.status,
            duration = System.currentTimeMillis() - startTime,
            extra = LogContextHolder.getAll()
        )
    }

    private fun shouldExclude(path: String): Boolean =
        properties.excludePaths.any { pattern -> pathMatcher.match(pattern, path) }

    companion object {
        private const val USER_AGENT_HEADER = "User-Agent"
    }
}
