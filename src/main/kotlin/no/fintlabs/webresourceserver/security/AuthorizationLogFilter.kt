package no.fintlabs.webresourceserver.security

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import java.io.IOException

class AuthorizationLogFilter : Filter {
    private val log = LoggerFactory.getLogger(AuthorizationLogFilter::class.java)

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        val httpRequest = request as HttpServletRequest
        log.trace("Authorization Header: {}", httpRequest.getHeader("Authorization"))
        chain.doFilter(request, response)
    }
}
