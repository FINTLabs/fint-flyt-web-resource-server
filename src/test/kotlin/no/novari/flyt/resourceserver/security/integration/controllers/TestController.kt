package no.novari.flyt.resourceserver.security.integration.controllers

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/**")
class TestController {

    @GetMapping
    fun getDummy(authentication: Authentication?): Mono<Set<String>> {
        if (authentication == null) {
            return Mono.empty()
        }
        val authorities = authentication.authorities
            .map(GrantedAuthority::getAuthority)
            .toSet()
        return Mono.just(authorities)
    }
}
