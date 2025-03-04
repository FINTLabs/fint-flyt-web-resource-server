package no.fintlabs.webresourceserver.security.properties

abstract class ApiSecurityProperties(
    var enabled: Boolean = false,
    var permitAll: Boolean = false,
) {

    abstract fun getPermittedAuthorities(): Array<String>

    protected fun mapToAuthoritiesArray(prefix: String, values: List<String>): Array<String> {
        return values.map { prefix + it }.toTypedArray()
    }
}