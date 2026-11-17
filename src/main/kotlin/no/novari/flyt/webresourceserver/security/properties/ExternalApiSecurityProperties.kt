package no.novari.flyt.webresourceserver.security.properties

public class ExternalApiSecurityProperties {
    public var enabled: Boolean = false
    public var authorizedSourceApplicationIds: Set<Long> = emptySet()
}
