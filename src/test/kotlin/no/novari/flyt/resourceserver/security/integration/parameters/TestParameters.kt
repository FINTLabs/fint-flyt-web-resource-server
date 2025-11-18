package no.novari.flyt.resourceserver.security.integration.parameters

data class TestParameters(
    val path: String,
    val tokenWrapper: TokenWrapper,
    val expectedResult: ExpectedResult,
) {
    override fun toString(): String {
        return listOf(
            path,
            "Token: ${tokenWrapper.tokenDescription}",
            "Exp result: $expectedResult",
        ).joinToString(" | ")
    }
}
