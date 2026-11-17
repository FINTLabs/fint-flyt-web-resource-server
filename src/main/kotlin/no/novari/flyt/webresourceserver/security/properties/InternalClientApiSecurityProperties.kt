package no.novari.flyt.webresourceserver.security.properties

public class InternalClientApiSecurityProperties {
    public var enabled: Boolean = false
    public var authorizedClientIds: Set<String> = emptySet()
}
