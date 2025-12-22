package no.novari.flyt.webresourceserver.security

import no.novari.flyt.webresourceserver.security.client.internal.InternalClientAuthorityMappingService
import no.novari.flyt.webresourceserver.security.client.internal.InternalClientJwtConverter
import no.novari.flyt.webresourceserver.security.properties.InternalClientApiSecurityProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "novari.flyt.web-resource-server.security.api",
    value = ["internal-client.enabled"],
    havingValue = "true",
)
class InternalClientApiConfiguration {
    @Bean
    @ConfigurationProperties("novari.flyt.web-resource-server.security.api.internal-client")
    fun internalClientApiSecurityProperties(): InternalClientApiSecurityProperties {
        return InternalClientApiSecurityProperties()
    }

    @Bean
    fun internalClientAuthorityMappingService(
        authorityMappingService: AuthorityMappingService,
    ): InternalClientAuthorityMappingService {
        return InternalClientAuthorityMappingService(authorityMappingService)
    }

    @Bean
    fun internalClientJwtConverter(
        internalClientAuthorityMappingService: InternalClientAuthorityMappingService,
    ): InternalClientJwtConverter {
        return InternalClientJwtConverter(internalClientAuthorityMappingService)
    }
}
