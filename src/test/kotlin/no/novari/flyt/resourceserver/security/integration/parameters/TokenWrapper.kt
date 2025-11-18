package no.novari.flyt.resourceserver.security.integration.parameters

import org.springframework.security.oauth2.jwt.Jwt

data class TokenWrapper(
    val tokenDescription: String,
    val token: Jwt?
) {
    companion object {
        fun none() = TokenWrapper("None", null)
    }
}
