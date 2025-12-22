package no.novari.flyt.webresourceserver.security.client.sourceapplication.exceptions

class MultipleSourceApplicationIdsException(
    sourceApplicationIds: Collection<Long>,
) : RuntimeException(
        "Source application IDs: ${
            sourceApplicationIds.joinToString(prefix = "[", postfix = "]")
        }",
    )
