package no.novari.flyt.webresourceserver.security.properties

class ExternalApiSecurityProperties {
    var enabled: Boolean = false
    var authorizedSourceApplicationIds: Set<Long> = emptySet()
}
