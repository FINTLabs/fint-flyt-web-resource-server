package no.novari.flyt.webresourceserver.security

import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationService
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class AuthorizationServiceConfiguration {
    @Bean
    fun sourceApplicationAuthorizationService(
        authorityMappingService: AuthorityMappingService,
    ): SourceApplicationAuthorizationService {
        return SourceApplicationAuthorizationService(authorityMappingService)
    }

    @Bean
    fun userAuthorizationService(authorityMappingService: AuthorityMappingService): UserAuthorizationService {
        return UserAuthorizationService(authorityMappingService)
    }
}
