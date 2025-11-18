package no.novari.flyt.resourceserver.security.client.internal

import no.novari.flyt.resourceserver.security.AuthorityMappingService
import no.novari.flyt.resourceserver.security.AuthorityPrefix
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class InternalClientAuthorityMappingService(
    private val authorityMappingService: AuthorityMappingService
) {

    fun createInternalClientIdAuthority(clientId: String): GrantedAuthority =
        SimpleGrantedAuthority(createInternalClientIdAuthorityString(clientId))

    fun createInternalClientIdAuthorityStrings(clientIds: Collection<String>): Set<String> =
        clientIds.mapTo(mutableSetOf(), ::createInternalClientIdAuthorityString)

    fun createInternalClientIdAuthorityString(clientId: String): String = authorityMappingService.toAuthority(
        AuthorityPrefix.CLIENT_ID,
        clientId
    )
}
