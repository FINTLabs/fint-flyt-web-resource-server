package no.fintlabs.webresourceserver.security

import no.fintlabs.webresourceserver.UrlPaths.INTERNAL_CLIENT_API
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$INTERNAL_CLIENT_API/dummy")
class InternalClientApiTestController {

    @GetMapping
    fun getDummy(): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }
}