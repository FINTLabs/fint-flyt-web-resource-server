package no.novari.flyt.resourceserver.security

import no.novari.flyt.resourceserver.UrlPaths
import no.novari.flyt.resourceserver.security.client.internal.InternalClientAuthorityMappingService
import no.novari.flyt.resourceserver.security.client.internal.InternalClientJwtConverter
import no.novari.flyt.resourceserver.security.client.sourceapplication.SourceApplicationAuthorityMappingService
import no.novari.flyt.resourceserver.security.client.sourceapplication.SourceApplicationJwtConverter
import no.novari.flyt.resourceserver.security.properties.ExternalApiSecurityProperties
import no.novari.flyt.resourceserver.security.properties.InternalClientApiSecurityProperties
import no.novari.flyt.resourceserver.security.user.UserJwtConverter
import no.novari.flyt.resourceserver.security.user.UserRole
import no.novari.flyt.resourceserver.security.user.UserRoleAuthorityMappingService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.security.authorization.AuthorityReactiveAuthorizationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@EnableWebFluxSecurity
@AutoConfiguration
class SecurityConfiguration {

    @Order(0)
    @Bean
    fun actuatorSecurityFilterChain(
        http: ServerHttpSecurity,
        securityWebFilterChainFactoryService: SecurityWebFilterChainFactoryService
    ): SecurityWebFilterChain = securityWebFilterChainFactoryService.permitAll(http, "/actuator")

    @Order(1)
    @Bean
    @ConditionalOnBean(InternalUserApiConfiguration::class)
    fun internalAdminApiFilterChain(
        http: ServerHttpSecurity,
        userJwtConverter: UserJwtConverter,
        userRoleAuthorityMappingService: UserRoleAuthorityMappingService,
        securityWebFilterChainFactoryService: SecurityWebFilterChainFactoryService
    ): SecurityWebFilterChain = securityWebFilterChainFactoryService.createFilterChain(
        http,
        UrlPaths.INTERNAL_ADMIN_API,
        userJwtConverter,
        AuthorityReactiveAuthorizationManager.hasAuthority(
            userRoleAuthorityMappingService.createRoleAuthorityString(UserRole.ADMIN)
        )
    )

    @Order(2)
    @Bean
    @ConditionalOnBean(InternalUserApiConfiguration::class)
    fun internalUserApiFilterChain(
        http: ServerHttpSecurity,
        userJwtConverter: UserJwtConverter,
        userRoleAuthorityMappingService: UserRoleAuthorityMappingService,
        securityWebFilterChainFactoryService: SecurityWebFilterChainFactoryService
    ): SecurityWebFilterChain = securityWebFilterChainFactoryService.createFilterChain(
        http,
        UrlPaths.INTERNAL_API,
        userJwtConverter,
        AuthorityReactiveAuthorizationManager.hasAuthority(
            userRoleAuthorityMappingService.createRoleAuthorityString(UserRole.USER)
        )
    )

    @Order(1)
    @Bean
    @ConditionalOnMissingBean(InternalUserApiConfiguration::class)
    fun internalApiDisabledFilterChain(
        http: ServerHttpSecurity,
        securityWebFilterChainFactoryService: SecurityWebFilterChainFactoryService
    ): SecurityWebFilterChain = securityWebFilterChainFactoryService.denyAll(http, UrlPaths.INTERNAL_API)

    @Order(3)
    @Bean
    @ConditionalOnBean(InternalClientApiConfiguration::class)
    fun internalClientApiFilterChain(
        http: ServerHttpSecurity,
        internalClientApiSecurityProperties: InternalClientApiSecurityProperties,
        internalClientJwtConverter: InternalClientJwtConverter,
        internalClientAuthorityMappingService: InternalClientAuthorityMappingService,
        securityWebFilterChainFactoryService: SecurityWebFilterChainFactoryService
    ): SecurityWebFilterChain = securityWebFilterChainFactoryService.createFilterChain(
        http,
        UrlPaths.INTERNAL_CLIENT_API,
        internalClientJwtConverter,
        AuthorityReactiveAuthorizationManager.hasAnyAuthority(
            *internalClientAuthorityMappingService.createInternalClientIdAuthorityStrings(
                internalClientApiSecurityProperties.authorizedClientIds
            ).toTypedArray()
        )
    )

    @Order(3)
    @Bean
    @ConditionalOnMissingBean(InternalClientApiConfiguration::class)
    fun internalClientApiDisabledFilterChain(
        http: ServerHttpSecurity,
        securityWebFilterChainFactoryService: SecurityWebFilterChainFactoryService
    ): SecurityWebFilterChain = securityWebFilterChainFactoryService.denyAll(http, UrlPaths.INTERNAL_CLIENT_API)

    @Order(4)
    @Bean
    @ConditionalOnBean(ExternalClientApiConfiguration::class)
    fun externalApiFilterChain(
        http: ServerHttpSecurity,
        externalApiSecurityProperties: ExternalApiSecurityProperties,
        sourceApplicationJwtConverter: SourceApplicationJwtConverter,
        sourceApplicationAuthorityMappingService: SourceApplicationAuthorityMappingService,
        securityWebFilterChainFactoryService: SecurityWebFilterChainFactoryService
    ): SecurityWebFilterChain = securityWebFilterChainFactoryService.createFilterChain(
        http,
        UrlPaths.EXTERNAL_API,
        sourceApplicationJwtConverter,
        AuthorityReactiveAuthorizationManager.hasAnyAuthority(
            *sourceApplicationAuthorityMappingService.createSourceApplicationAuthorityStrings(
                externalApiSecurityProperties.authorizedSourceApplicationIds
            ).toTypedArray()
        )
    )

    @Order(5)
    @Bean
    fun globalFilterChain(
        http: ServerHttpSecurity,
        securityWebFilterChainFactoryService: SecurityWebFilterChainFactoryService
    ): SecurityWebFilterChain = securityWebFilterChainFactoryService.denyAll(http)
}
