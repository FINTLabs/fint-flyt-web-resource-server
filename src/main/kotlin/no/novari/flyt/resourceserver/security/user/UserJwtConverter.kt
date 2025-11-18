package no.novari.flyt.resourceserver.security.user

import java.util.UUID
import no.novari.cache.FintCache
import no.novari.flyt.resourceserver.security.client.sourceapplication.SourceApplicationAuthorityMappingService
import no.novari.flyt.resourceserver.security.user.permission.UserPermission
import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import reactor.core.publisher.Mono

class UserJwtConverter(
    private val userPermissionCache: FintCache<UUID, UserPermission>,
    private val userRoleFilteringService: UserRoleFilteringService,
    private val sourceApplicationAuthorityMappingService: SourceApplicationAuthorityMappingService,
    private val userRoleHierarchyService: UserRoleHierarchyService,
    private val userRoleAuthorityMappingService: UserRoleAuthorityMappingService
) : Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    override fun convert(jwt: Jwt): Mono<AbstractAuthenticationToken> = Mono.fromCallable {
        val organizationId = jwt.getClaimAsString(UserClaim.ORGANIZATION_ID.tokenClaimName)
        log.debug("Extracted organization ID from JWT: {}", organizationId)

        val objectIdentifierString = jwt.getClaimAsString(UserClaim.OBJECT_IDENTIFIER.tokenClaimName)
        log.debug("Extracted objectIdentifier from JWT: {}", objectIdentifierString)

        if (organizationId.isNullOrBlank() || objectIdentifierString.isNullOrBlank()) {
            return@fromCallable JwtAuthenticationToken(jwt)
        }

        val objectIdentifier = UUID.fromString(objectIdentifierString)
        val authorities = mutableSetOf<GrantedAuthority>()

        userPermissionCache.getOptional(objectIdentifier)
            .map(UserPermission::sourceApplicationIds)
            .map(sourceApplicationAuthorityMappingService::createSourceApplicationAuthorities)
            .ifPresent(authorities::addAll)

        val roleValues = jwt.getClaimAsStringList(UserClaim.ROLES.tokenClaimName)
            ?.toSet()
            .orEmpty()
        log.debug("Extracted roles from JWT: {}", roleValues)

        if (roleValues.isNotEmpty()) {
            val filteredUserRoles = userRoleFilteringService.filter(roleValues, organizationId)
            val providedAndImpliedRoles = userRoleHierarchyService.getProvidedAndImpliedRoles(filteredUserRoles)
            authorities.addAll(userRoleAuthorityMappingService.createRoleAuthorities(providedAndImpliedRoles))
        }

        JwtAuthenticationToken(jwt, authorities)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(UserJwtConverter::class.java)
    }
}
