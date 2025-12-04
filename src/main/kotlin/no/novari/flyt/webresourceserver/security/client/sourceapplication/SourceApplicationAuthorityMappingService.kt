package no.novari.flyt.webresourceserver.security.client.sourceapplication

import no.novari.flyt.webresourceserver.security.AuthorityMappingService
import no.novari.flyt.webresourceserver.security.AuthorityPrefix
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service

@Service
class SourceApplicationAuthorityMappingService(
    private val authorityMappingService: AuthorityMappingService,
) {
    fun createSourceApplicationAuthorities(sourceApplicationIds: Collection<Long>): Set<GrantedAuthority> {
        return sourceApplicationIds.mapTo(mutableSetOf(), ::createSourceApplicationAuthority)
    }

    fun createSourceApplicationAuthority(sourceApplicationId: Long): GrantedAuthority {
        return SimpleGrantedAuthority(createSourceApplicationAuthorityString(sourceApplicationId))
    }

    fun createSourceApplicationAuthorityStrings(sourceApplicationIds: Collection<Long>): Set<String> {
        return sourceApplicationIds.mapTo(mutableSetOf(), ::createSourceApplicationAuthorityString)
    }

    fun createSourceApplicationAuthorityString(sourceApplicationId: Long): String {
        return authorityMappingService.toAuthority(
            AuthorityPrefix.SOURCE_APPLICATION_ID,
            sourceApplicationId.toString(),
        )
    }
}
