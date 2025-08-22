package no.fintlabs.webresourceserver.security.properties

import no.fintlabs.webresourceserver.security.client.ClientAuthorizationUtil.CLIENT_ID_PREFIX

class InternalClientApiSecurityProperties(
    val authorizedClientIds: MutableList<String> = mutableListOf(),
) : ApiSecurityProperties() {
    override fun getPermittedAuthorities(): Array<String> {
        return mapToAuthoritiesArray(CLIENT_ID_PREFIX, authorizedClientIds)
    }
}
