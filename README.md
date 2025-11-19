# FINT Flyt Web Resource Server

Auto-configuration for Spring Boot (Servlet stack) that standardizes OAuth2 resource-server behavior for FINT Flyt
services. It provisions dedicated `SecurityFilterChain`s for each internal/external API segment, enriches JWTs with
Kafka-backed permissions, and caches derived authorities so downstream applications can guard HTTP endpoints with
consistent semantics.

## Highlights

- **Segmented servlet filter chains** — `SecurityConfiguration` wires dedicated `SecurityFilterChain`s per API segment
  (actuator, internal admin/user, internal client, external) and applies the shared `AuthorizationLogFilter`.
- **Multi-tenant policy control** — API segments are toggled through `novari.flyt.resource-server.security.api.*`
  properties, allowing each service to expose only the surfaces needed by its organization.
- **Kafka-backed authorization** — `SourceApplicationAuthorizationRequestService` performs request/reply lookups for
  source-application IDs, while `UserPermissionCachingListenerFactory` streams user permissions into cache.
- **FINT cache integration** — Long-lived permission data is stored in a `FintCache` to avoid repeated Kafka round-trips
  and to speed up role checks.
- **Observability ready** — Ships with Spring Boot Actuator enabled so every host service inherits health and metrics
  probes under `/actuator/**`.

## Architecture Overview

| Component                                   | Responsibility                                                                                                     |
|---------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `SecurityConfiguration`                     | Wires the actuator, admin, user, internal-client, and external servlet filter chains with the proper matchers.     |
| `SecurityFilterChainFactoryService`         | Applies cross-cutting servlet security config (logging, CSRF off, JWT converter hookup, deny/permit helpers).      |
| `SourceApplicationJwtConverter`             | Converts external client JWTs into authorities by requesting authorization details over Kafka.                     |
| `InternalClientJwtConverter`                | Maps internal client subject claims to `CLIENT_ID_*` authorities used by the internal-client filter chain.         |
| `UserJwtConverter`                          | Enriches user tokens with org-filtered roles and cached source-application IDs retrieved from Kafka.               |
| `UserPermissionCachingListenerFactory`      | Builds a Kafka listener that keeps the `FintCache<UUID, UserPermission>` populated for the JWT converter.          |
| `SourceApplicationAuthorizationRequestService` | Manages Kafka request/reply infrastructure that resolves client IDs to source-application IDs.                   |
| `UserAuthorizationService`                  | Utility used by consumers of this starter to assert role membership or application-level access at runtime.        |

## HTTP API

Base paths guarded by the starter (see `UrlPaths`):

| Method | Path                     | Description                                                                                      | Request body                                   | Response                                                                 |
|--------|--------------------------|--------------------------------------------------------------------------------------------------|------------------------------------------------|--------------------------------------------------------------------------|
| `ANY`  | `/api/intern/admin/**`   | Internal admin API requiring at least the `ADMIN` role; disabled unless `internal.enabled=true`. | – (implemented by the host service)            | Protected downstream response, `401/403` when the JWT misses authorities. |
| `ANY`  | `/api/intern/**`         | Internal user API requiring the `USER` role; filters allowed roles per organization.             | –                                              | Same as above.                                                           |
| `ANY`  | `/api/intern-klient/**`  | Internal client API secured by specific client IDs embedded in the JWT subject.                  | –                                              | Same as above.                                                           |
| `ANY`  | `/api/**`                | External API for approved source applications announced through Kafka.                           | –                                              | Same as above.                                                           |

Tokens are expected to include the claims below; the starter extracts them to compute granted authorities:

```json
{
  "organizationid": "vtfk.no",
  "objectidentifier": "7d9e0d20-2a50-4ca1-9e4f-7c79f5fbe6c0",
  "roles": [
    "https://role-catalog.vigoiks.no/vigo/flyt/user"
  ],
  "sub": "client-123"
}
```

Errors fall back to Spring Security defaults: missing/invalid tokens result in `401 Unauthorized`, while denied
authorities answer with `403 Forbidden`.

## Kafka Integration

- `SourceApplicationAuthorizationRequestService` creates authorization request topics with the default org/application
  prefix, spins up short-lived reply topics (2-minute retention), and uses `RequestTemplate` to perform request/reply
  calls that translate client IDs to source-application IDs.
- `UserPermissionCachingListenerFactory` subscribes to the `userpermission` entity topic using
  `ParameterizedListenerContainerFactoryService`, writes records into the shared `FintCache`, and skips failed records
  through an `ErrorHandlerFactory`.
- Kafka connection, group IDs, and polling parameters rely on the shared `no.novari:kafka` helpers so the starter
  inherits FINT defaults (max poll settings, seek-to-beginning bootstrap, etc.).

## Scheduled Tasks

No scheduled jobs are defined; permission lifetimes are managed entirely through Kafka listeners and the configured cache
TTL (`novari.cache.default-cache-entry-time-to-live`) so no cron-like cleanup is needed.

## Configuration

Key properties exposed by the starter:

| Property                                                            | Description                                                                                     |
|---------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| `novari.flyt.resource-server.security.api.internal.enabled`         | Enables the internal admin/user APIs and registers the Kafka-backed user authorization beans.   |
| `novari.flyt.resource-server.security.api.internal.authorized-org-id-role-pairs-json` | JSON map of `{ "orgId": ["USER","ADMIN"] }` that filters allowed roles per organization.      |
| `novari.flyt.resource-server.security.api.internal-client.enabled`  | Turns on the internal client API filter chain.                                                  |
| `novari.flyt.resource-server.security.api.internal-client.authorized-client-ids` | List of JWT subjects that may call `/api/intern-klient/**`.                                  |
| `novari.flyt.resource-server.security.api.external.enabled`         | Turns on the external API filter chain.                                                         |
| `novari.flyt.resource-server.security.api.external.authorized-source-application-ids` | List of source-application IDs authorized for `/api/**`.                                     |
| `novari.kafka.application-id`                                       | Used for request/reply topic naming and listener group IDs.                                     |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri`              | Issuer for JWT validation (`https://idp.felleskomponent.no/nidp/oauth/nam`).                    |
| `novari.cache.default-cache-entry-time-to-live`                     | Default cache TTL (10 years by default) for cached permission entries.                          |

Secrets supplied via the consuming service’s deployment (Kubernetes secret, Vault, etc.) must provide OAuth client data,
Kafka bootstrap credentials, and any organization-specific JSON payloads mentioned above.

## Running Locally

Prerequisites:

- Java 21+
- Gradle (wrapper included)
- Kafka broker (for running the permission listener/request-reply interactions)

Useful commands:

```shell
./gradlew clean build         # compile and run the full test suite
./gradlew test                # unit tests only
./gradlew publishToMavenLocal # install the starter locally so other apps can depend on it during development
```

To experiment with the starter inside another Spring Boot service, add `no.novari:fint-flyt-web-resource-server` as a
dependency, run a local Kafka broker (e.g., docker-compose), and enable the desired API segment:

```shell
SPRING_APPLICATION_JSON='{
  "novari":{
    "flyt":{
      "resource-server":{
        "security":{
          "api":{
            "internal":{
              "enabled":true,
              "authorized-org-id-role-pairs-json":"{\"vtfk.no\":[\"USER\",\"ADMIN\"]}"
            }
          }
        }
      }
    },
    "kafka":{
      "application-id":"fint-flyt-web-resource-server-local"
    }
  }
}' ./gradlew bootRun
```

## Deployment

- Artifacts are published to `https://repo.fintlabs.no/releases` via `./gradlew publish`, reusing the Gradle publishing
  credentials (`REPOSILITE_USERNAME/PASSWORD`).
- Consumers pull the starter as a Maven dependency; Spring Boot automatically discovers the auto-configurations through
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Coordinate releases with downstream services so they can bump the dependency version and align Kafka/OAuth settings.

## Security

- Configures Spring Security’s servlet resource server with issuer validation against `https://idp.felleskomponent.no`.
- Path-level segmentation ensures `/api/intern*` endpoints stay internal while `/api/**` is limited to authorized source
  applications derived from Kafka.
- `AuthorityMappingService` and `AuthorityPrefix` standardize how authorities are encoded and later parsed by consumer
  services.
- `AuthorizationLogFilter` traces Authorization headers (at TRACE level) to help diagnose token issues without leaking
  them in higher log levels.
- Internal role evaluation filters token roles per organization and expands implied roles (Developer → Admin → User).

## Observability & Operations

- Health and readiness probes are inherited through Spring Boot Actuator (`/actuator/health`, `/actuator/readiness`,
  `/actuator/prometheus` when enabled by the host app).
- Cache state can be inspected via `FintCacheManager` metrics once Micrometer is enabled in the consuming service.
- Kafka listeners log consumed permission events at DEBUG so operators can trace authorization changes when needed.

## Development Tips

- When updating `authorized-org-id-role-pairs-json`, keep the payload valid JSON; the starter logs parse failures on
  startup.
- Use `UserAuthorizationService` inside host services to gate handlers the same way the filter chains do.
- Integration tests can mock `ReplyTopicService` and `RequestTemplateFactory` to avoid Kafka while still covering the
  JWT converters.
- `UserPermissionCachingListenerFactory` reuses default Kafka settings; override the listener configuration only if you
  truly need different poll sizes or retry logic.

## Contributing

1. Create a topic branch.
2. Run `./gradlew test` (or `./gradlew clean build`) before opening a PR.
3. Coordinate dependency version bumps with consumers and document any new configuration keys.
4. Add or adjust unit tests when changing converters, mapping services, or Kafka listeners.

———

FINT Flyt Web Resource Server is maintained by the FINT Flyt team. Reach out on the internal Slack channel or create an
issue in this repository for questions or enhancements.
