package no.novari.flyt.webresourceserver.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertySource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import java.io.IOException

/**
 * Loads the library's bundled flyt-web-resource-server-defaults.yml as default properties so consuming applications
 * get sensible defaults while still being able to override them.
 */
class ResourceServerDefaultsEnvironmentPostProcessor :
    EnvironmentPostProcessor,
    Ordered {
    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        if (!DEFAULTS.exists()) return

        try {
            val propertySources: List<PropertySource<*>> =
                YamlPropertySourceLoader().load(PROPERTY_SOURCE_NAME, DEFAULTS)
            propertySources.forEach { propertySource -> environment.propertySources.addLast(propertySource) }
        } catch (exception: IOException) {
            throw IllegalStateException(
                "Failed to load default properties from flyt-web-resource-server-defaults.yml",
                exception,
            )
        }
    }

    override fun getOrder(): Int = ConfigDataEnvironmentPostProcessor.ORDER + 1

    private companion object {
        private const val PROPERTY_SOURCE_NAME = "flyt-web-resource-server-defaults"
        private val DEFAULTS: Resource = ClassPathResource("flyt-web-resource-server-defaults.yml")
    }
}
