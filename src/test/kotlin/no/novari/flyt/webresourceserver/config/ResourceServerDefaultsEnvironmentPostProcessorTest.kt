package no.novari.flyt.webresourceserver.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

class ResourceServerDefaultsEnvironmentPostProcessorTest {
    @Test
    fun `authorization-service base-url includes the context path of the host app`() {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource("app", mapOf("server.servlet.context-path" to "/beta/afk-no")),
        )

        ResourceServerDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication())

        assertThat(environment.getProperty("novari.flyt.web-resource-server.security.authorization.base-url"))
            .isEqualTo("http://fint-flyt-authorization-service:8080/beta/afk-no")
    }
}
