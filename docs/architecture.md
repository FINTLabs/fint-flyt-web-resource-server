# Architecture

C4 level 1 to 3 for flyt-web-resource-server. Blue is this library or the services that embed it, turquoise is
external.

flyt-web-resource-server is a Spring Boot library, not a deployed service. Every FLYT backend service embeds it to
decide who may call which API:

| Segment                 | Callers             | Rule                                                                  |
|-------------------------|---------------------|-----------------------------------------------------------------------|
| `/api/intern/admin/**`  | FLYT users          | User token with the `ADMIN` role for the user's organisation          |
| `/api/intern/**`        | FLYT users          | User token with the `USER` role for the user's organisation           |
| `/api/intern-klient/**` | Other FLYT services | Client token whose subject is in `authorized-client-ids`              |
| `/api/**`               | Source applications | Client token whose client ID maps to an authorised source application |

Segments a service does not turn on are denied. `/actuator/**` is open.

## C4 level 1: System context

```mermaid
C4Context
    title System context for flyt-web-resource-server

    System_Ext(sourceApp, "Source application", "Acos, eGrunnerverv, HMSREG and others.")
    Person_Ext(user, "FLYT user", "Uses the FLYT frontend.")
    System(service, "FLYT service", "Any FLYT backend service with flyt-web-resource-server.")
    System_Ext(otherService, "Other FLYT service", "Calls this service service-to-service.")
    System_Ext(authz, "authorization-service", "Knows clients, source applications and user access.")
    System_Ext(idp, "NAM", "Issues OAuth2 tokens.")

    Rel(sourceApp, service, "External API", "HTTP")
    Rel(user, service, "Internal API", "HTTP")
    Rel(otherService, service, "Client API", "HTTP")
    Rel(service, authz, "Lookups", "HTTP")
    Rel(service, idp, "Tokens", "OIDC")

    UpdateRelStyle(sourceApp, service, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(user, service, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateRelStyle(otherService, service, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(service, authz, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(service, idp, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateElementStyle(service, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(sourceApp, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(user, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(otherService, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(authz, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(idp, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

NAM issues every token. The service validates incoming tokens against the issuer and gets its own tokens with
`client_credentials` for calls to authorization-service.

## C4 level 2: Containers

```mermaid
C4Container
    title Containers that embed flyt-web-resource-server

    System_Ext(sourceApp, "Source application", "Gets a token with its own client.")
    Container_Ext(frontend, "fint-flyt-frontend", "Web app", "UI for FLYT users.")
    Container(gateways, "Gateways", "Spring Boot, flyt-gateway-starter", "External API.")
    Container(internal, "Internal services", "Spring Boot", "Internal and internal client API.")
    Container_Ext(authz, "authorization-service", "Spring Boot", "/api/intern-klient/authorization")

    Rel(sourceApp, gateways, "Instances", "HTTP")
    Rel(frontend, internal, "Internal API", "HTTP")
    Rel(gateways, internal, "Client API", "HTTP")
    Rel(gateways, authz, "Client lookup", "HTTP")
    Rel(internal, authz, "User access", "HTTP")

    UpdateRelStyle(sourceApp, gateways, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(frontend, internal, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(gateways, internal, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(gateways, authz, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(internal, authz, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateElementStyle(gateways, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(internal, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(sourceApp, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(frontend, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(authz, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

| Container                                                                                   | Library via                 | Segments                  |
|---------------------------------------------------------------------------------------------|-----------------------------|---------------------------|
| acos, eapply, egrunnerverv, fskyss, hmsreg, isy-graving, side gateways and gateway-template | `flyt-gateway-starter`      | external                  |
| vigo-gateway                                                                                | `flyt-web-instance-gateway` | external                  |
| authorization-service, intgr-conf-service, archive-gateway                                  | direct                      | internal, internal-client |
| file-service                                                                                | direct                      | internal-client           |
| history, instance, discovery, integration, configuration, value-converting                  | direct                      | internal                  |
| mapping-service                                                                             | direct                      | none                      |

Calls to authorization-service use the OAuth2 registration `authorization-service` with the client
`fint-flyt-authorization-oauth2-client` (`fint.flyt.authorization.sso.*`). Base URL:
`http://fint-flyt-authorization-service:8080${server.servlet.context-path:}`. Timeouts come from
`spring.http.client.*` in the host app.

| Call                                                                                | Used for                       |
|-------------------------------------------------------------------------------------|--------------------------------|
| `GET /api/intern-klient/authorization/clients/{clientId}`                           | Every external call, cached 1m |
| `POST /api/intern-klient/authorization/users/actions/authorize-source-applications` | User access in handlers        |

## C4 level 3: Components

```mermaid
C4Component
    title Components in flyt-web-resource-server

    Container_Ext(caller, "Caller", "HTTP", "Source application, user or FLYT service.")
    Component(clientApi, "Internal client API", "JWT converter", "Client ID must be in authorized-client-ids.")
    Component(chains, "Filter chains", "Spring Security", "One chain per segment, deny for the rest.")
    Component(userApi, "User API", "JWT converter", "Roles per organisation, Developer to Admin to User.")
    Component(externalApi, "External API", "JWT converter, Caffeine", "Client ID to source application, cached 1m.")
    Component(userAuthz, "User authorization", "Service", "Called by handlers. 403 or 503.")
    Component(restClient, "Authorization client", "RestClient, OAuth2", "client_credentials against NAM.")
    Container_Ext(authz, "authorization-service", "Spring Boot", "/api/intern-klient/authorization")

    Rel(caller, chains, "JWT")
    Rel(chains, clientApi, "intern-klient")
    Rel(chains, userApi, "intern")
    Rel(chains, externalApi, "api")
    Rel(externalApi, restClient, "Client lookup")
    Rel(userAuthz, restClient, "User access")
    Rel(restClient, authz, "GET, POST", "HTTP")

    UpdateRelStyle(caller, chains, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(chains, clientApi, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateRelStyle(chains, userApi, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(chains, externalApi, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(externalApi, restClient, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(userAuthz, restClient, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateRelStyle(restClient, authz, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateElementStyle(clientApi, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(chains, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(userApi, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(externalApi, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(userAuthz, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(restClient, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(caller, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(authz, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

- External API: `404` from authorization-service is not cached and gives `401`. Other errors are not cached either,
  and the call is rejected.
- `AuthorityMappingService` encodes and parses the authorities all converters produce. It is left out of the diagram.
- Auto-configurations are listed in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  The external API and the authorization client need the `authorization-service` registration.
