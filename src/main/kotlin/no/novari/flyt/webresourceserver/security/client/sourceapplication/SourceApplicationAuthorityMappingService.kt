package no.novari.flyt.webresourceserver.security.client.sourceapplication

import no.novari.flyt.webresourceserver.security.AuthorityMappingService
import no.novari.flyt.webresourceserver.security.AuthorityPrefix
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service

@Service
public class SourceApplicationAuthorityMappingService(
    private val authorityMappingService: AuthorityMappingService,
) {
    public fun createSourceApplicationAuthorities(sourceApplicationIds: Collection<Long>): Set<GrantedAuthority> {
        return sourceApplicationIds.mapTo(mutableSetOf(), ::createSourceApplicationAuthority)
    }

    public fun createSourceApplicationAuthority(sourceApplicationId: Long): GrantedAuthority {
        return SimpleGrantedAuthority(createSourceApplicationAuthorityString(sourceApplicationId))
    }

    public fun createSourceApplicationAuthorityStrings(sourceApplicationIds: Collection<Long>): Set<String> {
        return sourceApplicationIds.mapTo(mutableSetOf(), ::createSourceApplicationAuthorityString)
    }

    public fun createSourceApplicationAuthorityString(sourceApplicationId: Long): String {
        return authorityMappingService.toAuthority(
            AuthorityPrefix.SOURCE_APPLICATION_ID,
            sourceApplicationId.toString(),
        )
    }
}
