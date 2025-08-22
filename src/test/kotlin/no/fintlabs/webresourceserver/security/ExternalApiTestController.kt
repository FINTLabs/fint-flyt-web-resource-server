package no.fintlabs.webresourceserver.security

import no.fintlabs.webresourceserver.UrlPaths.EXTERNAL_API
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$EXTERNAL_API/dummy")
class ExternalApiTestController {
    @GetMapping
    fun getDummy(): ResponseEntity<Void> {
        return ResponseEntity.ok().build()
    }
}
