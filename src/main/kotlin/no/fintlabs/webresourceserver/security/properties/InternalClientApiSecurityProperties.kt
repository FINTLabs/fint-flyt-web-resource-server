package no.fintlabs.webresourceserver.security.properties

import no.fintlabs.webresourceserver.security.client.ClientAuthorizationUtil.Companion.CLIENT_ID_PREFIX

class InternalClientApiSecurityProperties(
    private val authorizedClients: List<String> = emptyList(),
) : ApiSecurityProperties() {

    override fun getPermittedAuthorities(): Array<String> {
        return mapToAuthoritiesArray(CLIENT_ID_PREFIX, authorizedClients)
    }

}