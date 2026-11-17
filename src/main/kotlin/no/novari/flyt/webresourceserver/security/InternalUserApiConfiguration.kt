package no.novari.flyt.webresourceserver.security

import no.novari.flyt.webresourceserver.security.properties.InternalApiSecurityProperties
import no.novari.flyt.webresourceserver.security.user.UserJwtConverter
import no.novari.flyt.webresourceserver.security.user.UserRoleAuthorityMappingService
import no.novari.flyt.webresourceserver.security.user.UserRoleFilteringService
import no.novari.flyt.webresourceserver.security.user.UserRoleHierarchyService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "novari.flyt.web-resource-server.security.api",
    value = ["internal.enabled"],
    havingValue = "true",
)
public class InternalUserApiConfiguration {
    @Bean
    @ConfigurationProperties("novari.flyt.web-resource-server.security.api.internal")
    public fun internalApiSecurityProperties(): InternalApiSecurityProperties {
        return InternalApiSecurityProperties()
    }

    @Bean
    public fun userRoleFilteringService(
        internalApiSecurityProperties: InternalApiSecurityProperties,
    ): UserRoleFilteringService {
        return UserRoleFilteringService(internalApiSecurityProperties)
    }

    @Bean
    public fun userRoleAuthorityMappingService(
        authorityMappingService: AuthorityMappingService,
    ): UserRoleAuthorityMappingService {
        return UserRoleAuthorityMappingService(authorityMappingService)
    }

    @Bean
    public fun userRoleHierarchyService(): UserRoleHierarchyService {
        return UserRoleHierarchyService()
    }

    @Bean
    public fun userJwtConverter(
        userRoleFilteringService: UserRoleFilteringService,
        userRoleHierarchyService: UserRoleHierarchyService,
        userRoleAuthorityMappingService: UserRoleAuthorityMappingService,
    ): UserJwtConverter =
        UserJwtConverter(
            userRoleFilteringService,
            userRoleHierarchyService,
            userRoleAuthorityMappingService,
        )
}
