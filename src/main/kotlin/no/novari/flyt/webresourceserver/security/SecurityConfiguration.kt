package no.novari.flyt.webresourceserver.security

import no.novari.flyt.webresourceserver.UrlPaths
import no.novari.flyt.webresourceserver.security.client.internal.InternalClientAuthorityMappingService
import no.novari.flyt.webresourceserver.security.client.internal.InternalClientJwtConverter
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorityMappingService
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationJwtConverter
import no.novari.flyt.webresourceserver.security.properties.ExternalApiSecurityProperties
import no.novari.flyt.webresourceserver.security.properties.InternalClientApiSecurityProperties
import no.novari.flyt.webresourceserver.security.user.UserJwtConverter
import no.novari.flyt.webresourceserver.security.user.UserRole
import no.novari.flyt.webresourceserver.security.user.UserRoleAuthorityMappingService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.security.authorization.AuthorityAuthorizationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@EnableWebSecurity
@AutoConfiguration
class SecurityConfiguration {
    @Order(0)
    @Bean
    fun actuatorSecurityFilterChain(
        http: HttpSecurity,
        securityFilterChainFactoryService: SecurityFilterChainFactoryService,
    ): SecurityFilterChain = securityFilterChainFactoryService.permitAll(http, "/actuator")

    @Order(1)
    @Bean
    @ConditionalOnBean(InternalUserApiConfiguration::class)
    fun internalAdminApiFilterChain(
        http: HttpSecurity,
        userJwtConverter: UserJwtConverter,
        userRoleAuthorityMappingService: UserRoleAuthorityMappingService,
        securityFilterChainFactoryService: SecurityFilterChainFactoryService,
    ): SecurityFilterChain =
        securityFilterChainFactoryService.createFilterChain(
            http,
            UrlPaths.INTERNAL_ADMIN_API,
            userJwtConverter,
            AuthorityAuthorizationManager.hasAuthority(
                userRoleAuthorityMappingService.createRoleAuthorityString(UserRole.ADMIN),
            ),
        )

    @Order(2)
    @Bean
    @ConditionalOnBean(InternalUserApiConfiguration::class)
    fun internalUserApiFilterChain(
        http: HttpSecurity,
        userJwtConverter: UserJwtConverter,
        userRoleAuthorityMappingService: UserRoleAuthorityMappingService,
        securityFilterChainFactoryService: SecurityFilterChainFactoryService,
    ): SecurityFilterChain =
        securityFilterChainFactoryService.createFilterChain(
            http,
            UrlPaths.INTERNAL_API,
            userJwtConverter,
            AuthorityAuthorizationManager.hasAuthority(
                userRoleAuthorityMappingService.createRoleAuthorityString(UserRole.USER),
            ),
        )

    @Order(1)
    @Bean
    @ConditionalOnMissingBean(InternalUserApiConfiguration::class)
    fun internalApiDisabledFilterChain(
        http: HttpSecurity,
        securityFilterChainFactoryService: SecurityFilterChainFactoryService,
    ): SecurityFilterChain = securityFilterChainFactoryService.denyAll(http, UrlPaths.INTERNAL_API)

    @Order(3)
    @Bean
    @ConditionalOnBean(InternalClientApiConfiguration::class)
    fun internalClientApiFilterChain(
        http: HttpSecurity,
        internalClientApiSecurityProperties: InternalClientApiSecurityProperties,
        internalClientJwtConverter: InternalClientJwtConverter,
        internalClientAuthorityMappingService: InternalClientAuthorityMappingService,
        securityFilterChainFactoryService: SecurityFilterChainFactoryService,
    ): SecurityFilterChain =
        securityFilterChainFactoryService.createFilterChain(
            http,
            UrlPaths.INTERNAL_CLIENT_API,
            internalClientJwtConverter,
            AuthorityAuthorizationManager.hasAnyAuthority(
                *internalClientAuthorityMappingService
                    .createInternalClientIdAuthorityStrings(
                        internalClientApiSecurityProperties.authorizedClientIds,
                    ).toTypedArray(),
            ),
        )

    @Order(3)
    @Bean
    @ConditionalOnMissingBean(InternalClientApiConfiguration::class)
    fun internalClientApiDisabledFilterChain(
        http: HttpSecurity,
        securityFilterChainFactoryService: SecurityFilterChainFactoryService,
    ): SecurityFilterChain = securityFilterChainFactoryService.denyAll(http, UrlPaths.INTERNAL_CLIENT_API)

    @Order(4)
    @Bean
    @ConditionalOnBean(ExternalClientApiConfiguration::class)
    fun externalApiFilterChain(
        http: HttpSecurity,
        externalApiSecurityProperties: ExternalApiSecurityProperties,
        sourceApplicationJwtConverter: SourceApplicationJwtConverter,
        sourceApplicationAuthorityMappingService: SourceApplicationAuthorityMappingService,
        securityFilterChainFactoryService: SecurityFilterChainFactoryService,
    ): SecurityFilterChain =
        securityFilterChainFactoryService.createFilterChain(
            http,
            UrlPaths.EXTERNAL_API,
            sourceApplicationJwtConverter,
            AuthorityAuthorizationManager.hasAnyAuthority(
                *sourceApplicationAuthorityMappingService
                    .createSourceApplicationAuthorityStrings(
                        externalApiSecurityProperties.authorizedSourceApplicationIds,
                    ).toTypedArray(),
            ),
        )

    @Order(5)
    @Bean
    fun globalFilterChain(
        http: HttpSecurity,
        securityFilterChainFactoryService: SecurityFilterChainFactoryService,
    ): SecurityFilterChain = securityFilterChainFactoryService.denyAll(http)
}
