package no.novari.flyt.resourceserver.security.integration.controllers

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/**")
class TestController {

    @GetMapping
    fun getDummy(authentication: Authentication?): ResponseEntity<Set<String>> {
        if (authentication == null) {
            return ResponseEntity.ok(emptySet())
        }
        val authorities = authentication.authorities
            .map(GrantedAuthority::getAuthority)
            .toSet()
        return ResponseEntity.ok(authorities)
    }
}
