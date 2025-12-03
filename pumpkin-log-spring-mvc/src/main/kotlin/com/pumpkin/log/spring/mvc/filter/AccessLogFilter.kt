package com.pumpkin.log.spring.mvc.filter

import com.pumpkin.log.context.LogContextHolder
import com.pumpkin.log.logger.AccessLogger
import com.pumpkin.log.spring.mvc.config.PumpkinLogProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

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
            val duration = System.currentTimeMillis() - startTime
            val extra = LogContextHolder.getAll()

            accessLogger.log(
                userAgent = request.getHeader("User-Agent") ?: "",
                method = request.method,
                path = request.requestURI,
                query = request.queryString ?: "",
                statusCode = response.status,
                duration = duration,
                extra = extra
            )

            LogContextHolder.clear()
        }
    }

    private fun shouldExclude(path: String): Boolean {
        return properties.excludePaths.any { pattern ->
            pathMatcher.match(pattern, path)
        }
    }
}
