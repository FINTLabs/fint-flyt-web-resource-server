package no.novari.flyt.resourceserver.security.integration.values

enum class ClientId(val claimValue: String) {
    WITH_EXTERNAL_CLIENT_SA_AUTHORIZATION_ID_1("1ba1079e-a60d-470c-a814-ebc1c97140fb"),
    WITH_NO_EXTERNAL_CLIENT_SA_AUTHORIZATION("b8d02b93-bc02-4664-8c19-1cc750db7403"),
    AUTHORIZED_FOR_INTERNAL_CLIENT_API("9e8118f3-9bc0-4f00-8675-c04bf8fe2494"),
    NOT_AUTHORIZED_FOR_INTERNAL_CLIENT_API("3d416475-0b4b-473a-b5a2-7742f5e68391")
}
