package no.novari.flyt.resourceserver.security

import java.time.Duration
import java.util.UUID
import no.novari.cache.FintCache
import no.novari.cache.FintCacheConfiguration
import no.novari.cache.FintCacheManager
import no.novari.cache.FintCacheOptions
import no.novari.flyt.resourceserver.security.client.sourceapplication.SourceApplicationAuthorityMappingService
import no.novari.flyt.resourceserver.security.properties.InternalApiSecurityProperties
import no.novari.flyt.resourceserver.security.user.UserJwtConverter
import no.novari.flyt.resourceserver.security.user.UserRoleAuthorityMappingService
import no.novari.flyt.resourceserver.security.user.UserRoleFilteringService
import no.novari.flyt.resourceserver.security.user.UserRoleHierarchyService
import no.novari.flyt.resourceserver.security.user.permission.UserPermission
import no.novari.flyt.resourceserver.security.user.permission.UserPermissionCachingListenerFactory
import no.novari.kafka.consuming.ErrorHandlerFactory
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "novari.flyt.resource-server.security.api",
    value = ["internal.enabled"],
    havingValue = "true"
)
@Import(FintCacheConfiguration::class)
class InternalUserApiConfiguration {

    @Bean
    @ConfigurationProperties("novari.flyt.resource-server.security.api.internal")
    fun internalApiSecurityProperties() = InternalApiSecurityProperties()

    @Bean
    fun userRoleFilteringService(
        internalApiSecurityProperties: InternalApiSecurityProperties
    ) = UserRoleFilteringService(internalApiSecurityProperties)

    @Bean
    fun userPermissionCache(
        fintCacheManager: FintCacheManager
    ): FintCache<UUID, UserPermission> = fintCacheManager.createCache(
        "userpermission",
        UUID::class.java,
        UserPermission::class.java,
        FintCacheOptions.builder()
            .timeToLive(Duration.ofMillis(Long.MAX_VALUE))
            .heapSize(1_000_000L)
            .build()
    )

    @Bean
    fun userRoleAuthorityMappingService(
        authorityMappingService: AuthorityMappingService
    ) = UserRoleAuthorityMappingService(authorityMappingService)

    @Bean
    fun userRoleHierarchyService() = UserRoleHierarchyService()

    @Bean
    fun userJwtConverter(
        userPermissionCache: FintCache<UUID, UserPermission>,
        userRoleFilteringService: UserRoleFilteringService,
        sourceApplicationAuthorityMappingService: SourceApplicationAuthorityMappingService,
        userRoleHierarchyService: UserRoleHierarchyService,
        userRoleAuthorityMappingService: UserRoleAuthorityMappingService
    ) = UserJwtConverter(
        userPermissionCache,
        userRoleFilteringService,
        sourceApplicationAuthorityMappingService,
        userRoleHierarchyService,
        userRoleAuthorityMappingService
    )

    @Bean("userPermissionCachingListener")
    fun userPermissionCachingListener(
        containerFactoryService: ParameterizedListenerContainerFactoryService,
        userPermissionCache: FintCache<UUID, UserPermission>,
        errorHandlerFactory: ErrorHandlerFactory
    ): ConcurrentMessageListenerContainer<String, UserPermission> =
        UserPermissionCachingListenerFactory().create(
            containerFactoryService,
            userPermissionCache,
            errorHandlerFactory
        )
}
