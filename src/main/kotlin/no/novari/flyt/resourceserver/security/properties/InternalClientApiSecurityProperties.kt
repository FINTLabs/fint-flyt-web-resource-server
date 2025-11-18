package no.novari.flyt.resourceserver.security.properties

class InternalClientApiSecurityProperties {
    var enabled: Boolean = false
    var authorizedClientIds: Set<String> = emptySet()
}
