package no.novari.flyt.webresourceserver.security

public enum class AuthorityPrefix(
    public val value: String,
) {
    ORG_ID("ORG_ID"),
    ROLE("ROLE"),
    SOURCE_APPLICATION_ID("SOURCE_APPLICATION_ID"),
    CLIENT_ID("CLIENT_ID"),
}
