package no.novari.flyt.webresourceserver.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter

class AuthorizationLogFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        request.getHeader("Authorization")?.let { log.trace(it) }
        filterChain.doFilter(request, response)
    }

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
