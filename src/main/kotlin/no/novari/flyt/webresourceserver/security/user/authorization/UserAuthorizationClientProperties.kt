package no.novari.flyt.webresourceserver.security.user.authorization

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "novari.flyt.web-resource-server.security.authorization")
data class UserAuthorizationClientProperties(
    val baseUrl: String = "http://fint-flyt-authorization-service:8080",
    val clientRegistrationId: String = "authorization-service",
    val cache: Cache = Cache(),
) {
    data class Cache(
        val ttl: Duration = Duration.ofSeconds(15),
        val maxSize: Long = 10_000,
        val staleIfError: Duration = Duration.ofMinutes(2),
    )
}
