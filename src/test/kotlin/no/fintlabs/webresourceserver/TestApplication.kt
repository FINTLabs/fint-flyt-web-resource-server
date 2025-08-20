package no.fintlabs.webresourceserver

import no.fintlabs.webresourceserver.security.SecurityConfiguration
import no.fintlabs.webresourceserver.security.client.ClientJwtConverter
import no.fintlabs.webresourceserver.security.client.sourceapplication.SourceApplicationJwtConverter
import no.fintlabs.webresourceserver.security.user.userpermission.UserPermissionConsumerConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

@SpringBootApplication
@ComponentScan(
    basePackageClasses = [
        ClientJwtConverter::class,
        SourceApplicationJwtConverter::class,
        SecurityConfiguration::class,
    ],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [UserPermissionConsumerConfiguration::class],
        ),
    ],
)
class TestApplication

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args)
}
