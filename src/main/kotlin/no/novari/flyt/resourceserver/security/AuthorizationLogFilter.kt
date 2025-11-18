package no.novari.flyt.resourceserver.security

import org.slf4j.LoggerFactory
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class AuthorizationLogFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        log.trace(exchange.request.headers["Authorization"].toString())
        return chain.filter(exchange)
    }

    companion object {
        private val log = LoggerFactory.getLogger(AuthorizationLogFilter::class.java)
    }
}
