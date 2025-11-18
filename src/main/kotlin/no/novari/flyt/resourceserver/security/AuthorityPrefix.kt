package no.novari.flyt.resourceserver.security

enum class AuthorityPrefix(val value: String) {
    ORG_ID("ORG_ID"),
    ROLE("ROLE"),
    SOURCE_APPLICATION_ID("SOURCE_APPLICATION_ID"),
    CLIENT_ID("CLIENT_ID"),
}
