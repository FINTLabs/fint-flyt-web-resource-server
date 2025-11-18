package no.novari.flyt.resourceserver.security.properties

class ExternalApiSecurityProperties {
    var enabled: Boolean = false
    var authorizedSourceApplicationIds: Set<Long> = emptySet()
}
