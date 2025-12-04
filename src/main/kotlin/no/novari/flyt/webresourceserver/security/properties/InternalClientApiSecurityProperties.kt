package no.novari.flyt.webresourceserver.security.properties

class InternalClientApiSecurityProperties {
    var enabled: Boolean = false
    var authorizedClientIds: Set<String> = emptySet()
}
