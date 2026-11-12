package no.novari.flyt.webresourceserver.security.client.sourceapplication.exceptions

public class MultipleSourceApplicationIdsException(
    sourceApplicationIds: Collection<Long>,
) : RuntimeException(
        "Source application IDs: ${
            sourceApplicationIds.joinToString(prefix = "[", postfix = "]")
        }",
    )
