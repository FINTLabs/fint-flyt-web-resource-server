package no.novari.flyt.webresourceserver.security

import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorityMappingService
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationClient
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationJwtConverter
import no.novari.flyt.webresourceserver.security.properties.ExternalApiSecurityProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestClient

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "novari.flyt.web-resource-server.security.api",
    value = ["external.enabled"],
    havingValue = "true",
)
internal class ExternalClientApiConfiguration {
    @Bean
    @ConfigurationProperties("novari.flyt.web-resource-server.security.api.external")
    fun externalApiSecurityProperties() = ExternalApiSecurityProperties()

    // Requires the authorization-service OAuth2 client registration, see AuthorizationServiceConfiguration.
    @Bean
    fun sourceApplicationAuthorizationClient(
        @Qualifier("authorizationServiceRestClient") restClient: RestClient,
    ) = SourceApplicationAuthorizationClient(restClient)

    @Bean
    fun sourceApplicationJwtConverter(
        sourceApplicationAuthorizationClient: SourceApplicationAuthorizationClient,
        sourceApplicationAuthorityMappingService: SourceApplicationAuthorityMappingService,
    ) = SourceApplicationJwtConverter(sourceApplicationAuthorizationClient, sourceApplicationAuthorityMappingService)
}
