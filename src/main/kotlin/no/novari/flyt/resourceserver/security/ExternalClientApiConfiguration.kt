package no.novari.flyt.resourceserver.security

import no.novari.flyt.resourceserver.security.properties.ExternalApiSecurityProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "novari.flyt.resource-server.security.api",
    value = ["external.enabled"],
    havingValue = "true"
)
class ExternalClientApiConfiguration {

    @Bean
    @ConfigurationProperties("novari.flyt.resource-server.security.api.external")
    fun externalApiSecurityProperties() = ExternalApiSecurityProperties()
}
