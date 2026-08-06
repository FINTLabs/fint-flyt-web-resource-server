package no.novari.flyt.webresourceserver.security.user.authorization

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "novari.flyt.web-resource-server.security.authorization")
data class UserAuthorizationClientProperties(
    val baseUrl: String = "http://fint-flyt-authorization-service:8080",
    val clientRegistrationId: String = "authorization-service",
)
