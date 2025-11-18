package no.novari.flyt.resourceserver.security.integration.parameters

import org.springframework.http.HttpStatus

data class ExpectedResult(
    val status: HttpStatus,
    val authorities: Set<String>? = null
) {
    override fun toString(): String = buildString {
        append('{')
        append(status)
        authorities?.let { append(", Authz: ").append(it) }
        append('}')
    }
}
