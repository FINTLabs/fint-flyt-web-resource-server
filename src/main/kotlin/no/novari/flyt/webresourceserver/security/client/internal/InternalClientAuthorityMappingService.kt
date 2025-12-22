package no.novari.flyt.webresourceserver.security.client.internal

import no.novari.flyt.webresourceserver.security.AuthorityMappingService
import no.novari.flyt.webresourceserver.security.AuthorityPrefix
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class InternalClientAuthorityMappingService(
    private val authorityMappingService: AuthorityMappingService,
) {
    fun createInternalClientIdAuthority(clientId: String): GrantedAuthority {
        return SimpleGrantedAuthority(createInternalClientIdAuthorityString(clientId))
    }

    fun createInternalClientIdAuthorityStrings(clientIds: Collection<String>): Set<String> {
        return clientIds.mapTo(mutableSetOf(), ::createInternalClientIdAuthorityString)
    }

    fun createInternalClientIdAuthorityString(clientId: String): String {
        return authorityMappingService.toAuthority(
            AuthorityPrefix.CLIENT_ID,
            clientId,
        )
    }
}
