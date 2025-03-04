package no.fintlabs.webresourceserver.security.properties

import no.fintlabs.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationService.Companion.SOURCE_APPLICATION_ID_PREFIX

class ExternalApiSecurityProperties (
    private var authorizedClients: List<String> = emptyList()
) : ApiSecurityProperties() {

    override fun getPermittedAuthorities(): Array<String> {
        return mapToAuthoritiesArray(SOURCE_APPLICATION_ID_PREFIX, authorizedClients)
    }

}