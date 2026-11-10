package no.novari.flyt.webresourceserver.security

import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorityMappingService
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationJwtConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

class ExternalClientApiConfigurationTest {
    private val contextRunner =
        WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    OAuth2ClientAutoConfiguration::class.java,
                    HttpClientAutoConfiguration::class.java,
                    RestClientAutoConfiguration::class.java,
                    AuthorizationServiceConfiguration::class.java,
                    ExternalClientApiConfiguration::class.java,
                ),
            ).withBean(AuthorityMappingService::class.java)
            .withBean(SourceApplicationAuthorityMappingService::class.java)

    private val registration =
        arrayOf(
            "spring.security.oauth2.client.provider.fint-idp.token-uri=http://idp/token",
            "spring.security.oauth2.client.registration.authorization-service.provider=fint-idp",
            "spring.security.oauth2.client.registration.authorization-service.authorization-grant-type=" +
                "client_credentials",
            "spring.security.oauth2.client.registration.authorization-service.client-id=client",
            "spring.security.oauth2.client.registration.authorization-service.client-secret=secret",
        )

    @Test
    fun `external API uses the authorization-service registration`() {
        contextRunner
            .withPropertyValues(EXTERNAL_ENABLED, *registration)
            .run { context -> assertThat(context).hasSingleBean(SourceApplicationJwtConverter::class.java) }
    }

    @Test
    fun `external API fails at startup without the authorization-service registration`() {
        contextRunner
            .withPropertyValues(EXTERNAL_ENABLED)
            .run { context ->
                assertThat(context)
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("authorizationServiceRestClient")
            }
    }

    @Test
    fun `source application lookup is not created when the external API is off`() {
        contextRunner
            .withPropertyValues(*registration)
            .run { context -> assertThat(context).doesNotHaveBean(SourceApplicationJwtConverter::class.java) }
    }

    private companion object {
        const val EXTERNAL_ENABLED = "novari.flyt.web-resource-server.security.api.external.enabled=true"
    }
}
