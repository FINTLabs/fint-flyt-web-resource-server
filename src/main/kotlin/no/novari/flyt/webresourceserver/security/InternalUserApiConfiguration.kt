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
class InternalUserApiConfiguration {
    @Bean
    @ConfigurationProperties("novari.flyt.web-resource-server.security.api.internal")
    fun internalApiSecurityProperties(): InternalApiSecurityProperties {
        return InternalApiSecurityProperties()
    }

    @Bean
    fun userRoleFilteringService(
        internalApiSecurityProperties: InternalApiSecurityProperties,
    ): UserRoleFilteringService {
        return UserRoleFilteringService(internalApiSecurityProperties)
    }

    @Bean
    fun userRoleAuthorityMappingService(
        authorityMappingService: AuthorityMappingService,
    ): UserRoleAuthorityMappingService {
        return UserRoleAuthorityMappingService(authorityMappingService)
    }

    @Bean
    fun userRoleHierarchyService(): UserRoleHierarchyService {
        return UserRoleHierarchyService()
    }

    @Bean
    fun userJwtConverter(
        userRoleFilteringService: UserRoleFilteringService,
        userRoleHierarchyService: UserRoleHierarchyService,
        userRoleAuthorityMappingService: UserRoleAuthorityMappingService,
    ) = UserJwtConverter(
        userRoleFilteringService,
        userRoleHierarchyService,
        userRoleAuthorityMappingService,
    )
}
