package no.fintlabs.webresourceserver.security

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import java.io.IOException

class AuthorizationLogFilter : Filter {
    companion object {
        private val log = LoggerFactory.getLogger(AuthorizationLogFilter::class.java)
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        val header = (request as? HttpServletRequest)?.getHeader(HttpHeaders.AUTHORIZATION)
        log.trace("Authorization Header: {}", header)
        chain.doFilter(request, response)
    }
}
