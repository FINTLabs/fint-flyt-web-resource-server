package no.novari.flyt.resourceserver.security

import no.novari.flyt.resourceserver.security.client.sourceapplication.SourceApplicationAuthorizationService
import no.novari.flyt.resourceserver.security.user.UserAuthorizationService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class AuthorizationServiceConfiguration {

    @Bean
    fun sourceApplicationAuthorizationService(
        authorityMappingService: AuthorityMappingService
    ) = SourceApplicationAuthorizationService(authorityMappingService)

    @Bean
    fun userAuthorizationService(
        authorityMappingService: AuthorityMappingService
    ) = UserAuthorizationService(authorityMappingService)
}
