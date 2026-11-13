# FINT Flyt Web Resource Server

Spring Boot auto-configuration (servlet stack) that standardises OAuth2 resource server behaviour for FINT Flyt
services. It sets up a dedicated `SecurityFilterChain` for each internal and external API segment, maps roles and
claims from JWTs, and lets host services check user access against authorization-service over HTTP.

## Highlights

- **Segmented servlet filter chains:** `SecurityConfiguration` sets up one `SecurityFilterChain` per API segment
  (actuator, internal admin/user, internal client, external).
- **Per-service API segments:** segments are switched on with `novari.flyt.web-resource-server.security.api.*`, so each
  service exposes only what it needs.
- **User authorization over HTTP:** `UserAuthorizationService` uses OAuth2 client credentials against
  authorization-service for each `(user, source application)` check.
- **Source application authorization over HTTP:** `SourceApplicationJwtConverter` looks up the client ID in
  authorization-service and caches hits for one minute.

## Architecture

| Component                              | Responsibility                                                                                       |
| -------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `SecurityConfiguration`                | Servlet filter chains for actuator, admin, user, internal-client and external, with their matchers.  |
| `SecurityFilterChainFactoryService`    | Shared servlet security setup: CSRF off, JWT converter wiring, deny/permit helpers.                  |
| `SourceApplicationJwtConverter`        | Converts external client JWTs to authorities via `SourceApplicationAuthorizationClient`.             |
| `InternalClientJwtConverter`           | Maps internal client subject claims to `CLIENT_ID_*` authorities for the internal-client chain.      |
| `UserJwtConverter`                     | Adds org-filtered roles to user tokens. Source application access is not fetched here.               |
| `SourceApplicationAuthorizationClient` | Fetches the source application for a client ID from authorization-service and caches hits for 1 min. |
| `UserAuthorizationService`             | Used by host services for role and source application checks at runtime.                             |
| `RestClientUserAuthorizationClient`    | Calls authorization-service for each user authorization check.                                       |

## HTTP API

Base paths protected by the library (see `UrlPaths`):

| Method | Path                    | Description                                                                        | Request body                | Response                                                          |
| ------ | ----------------------- | ---------------------------------------------------------------------------------- | --------------------------- | ----------------------------------------------------------------- |
| `ANY`  | `/api/intern/admin/**`  | Internal admin API, requires at least `ADMIN`. Off unless `internal.enabled=true`. | – (implemented by the host) | Response from the host, `401/403` when the JWT lacks authorities. |
| `ANY`  | `/api/intern/**`        | Internal user API, requires `USER`. Allowed roles are filtered per organisation.   | –                           | Same as above.                                                    |
| `ANY`  | `/api/intern-klient/**` | Internal client API, restricted to specific client IDs in the JWT subject.         | –                           | Same as above.                                                    |
| `ANY`  | `/api/**`               | External API for source applications whose client ID authorization-service knows.  | –                           | Same as above.                                                    |

Tokens are expected to contain these claims, which are used to compute the granted authorities:

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

Errors follow Spring Security defaults: a missing or invalid token gives `401 Unauthorized`, missing authorities give
`403 Forbidden`.

## Authorization over HTTP

User authorization uses `client_credentials` against
`POST /api/intern-klient/authorization/users/actions/authorize-source-applications` in authorization-service. The host
sends an object identifier and candidate source application IDs; the response is the allowed subset. A single check
gives `403`, and errors or an unavailable authorization-service give `503`.

Source application authorization calls `GET /api/intern-klient/authorization/clients/{clientId}` on each call to
`/api/**`. Hits are cached for one minute. `404` is not cached and gives `401`. Other errors are not cached either, and
the call is rejected.

## Setup for apps with the external API

An app with `novari.flyt.web-resource-server.security.api.external.enabled=true` needs the OAuth client
`fint-flyt-authorization-oauth2-client` and the registration `authorization-service`. Without the registration the app
fails at startup.

Add the secret with `env`, not `envFrom`. It exposes `fint.sso.*`, which collides with other OAuth clients in the pod,
such as `fint-flyt-file-oauth2-client` in the gateways:

```yaml
# kustomize/base/oauth2-authorization-client.yaml
apiVersion: "fintlabs.no/v1alpha1"
kind: NamOAuthClientApplicationResource
metadata:
  name: fint-flyt-authorization-oauth2-client
spec:
  grantTypes:
    - client_credentials
```

```yaml
# kustomize/base/flais.yaml, under spec.env
- name: fint.flyt.authorization.sso.client-id
  valueFrom:
    secretKeyRef:
      name: fint-flyt-authorization-oauth2-client
      key: fint.sso.client-id
- name: fint.flyt.authorization.sso.client-secret
  valueFrom:
    secretKeyRef:
      name: fint-flyt-authorization-oauth2-client
      key: fint.sso.client-secret
```

In `ra-no` the secret keys are `fint.flyt.authorization.sso.client-id` and `fint.flyt.authorization.sso.client-secret`,
so the overlay must patch `secretKeyRef.key`, as intgr-conf-service does.

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          fint-idp:
            token-uri: https://idp.felleskomponent.no/nidp/oauth/nam/token
        registration:
          authorization-service:
            authorization-grant-type: client_credentials
            client-id: ${fint.flyt.authorization.sso.client-id}
            client-secret: ${fint.flyt.authorization.sso.client-secret}
            provider: fint-idp
```

Calls to authorization-service and the token request to NAM have a 2s connect and 5s read timeout. If the app sets
`spring.http.client.connect-timeout` or `spring.http.client.read-timeout`, those apply instead.

## Configuration

| Property                                                                                  | Description                                                                                                            |
| ----------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `novari.flyt.web-resource-server.security.api.internal.enabled`                           | Enables the internal admin/user APIs. User authorization beans are created when the OAuth2 registration exists.        |
| `novari.flyt.web-resource-server.security.api.internal.authorized-org-id-role-pairs-json` | JSON map `{ "orgId": ["USER","ADMIN"] }` that filters allowed roles per organisation.                                  |
| `novari.flyt.web-resource-server.security.api.internal-client.enabled`                    | Enables the internal client API filter chain.                                                                          |
| `novari.flyt.web-resource-server.security.api.internal-client.authorized-client-ids`      | JWT subjects allowed to call `/api/intern-klient/**`.                                                                  |
| `novari.flyt.web-resource-server.security.api.external.enabled`                           | Enables the external API filter chain.                                                                                 |
| `novari.flyt.web-resource-server.security.api.external.authorized-source-application-ids` | Source application IDs allowed on `/api/**`.                                                                           |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri`                                    | Issuer for JWT validation (`https://idp.felleskomponent.no/nidp/oauth/nam`).                                           |
| `novari.flyt.web-resource-server.security.authorization.base-url`                         | authorization-service base URL (default `http://fint-flyt-authorization-service:8080${server.servlet.context-path:}`). |
| `novari.flyt.web-resource-server.security.authorization.client-registration-id`           | OAuth2 client registration (default `authorization-service`).                                                          |

Services that use `UserAuthorizationService` or the external API must provide client credentials for the
`authorization-service` registration.

## Upgrading from 4.x

- `no.novari:kafka` and `spring-kafka` are no longer transitive. Apps that use Kafka must declare them.
- `SourceApplicationAuthorizationRequestService` is removed. Remove its mock from tests. If a test needs the external
  API, mock `SourceApplicationJwtConverter` instead.
- `SourceApplicationJwtConverter` is only created when the external API is on.
- `userAuthorizationRestClient` is renamed to `authorizationServiceRestClient`. It no longer uses a
  `ClientHttpRequestFactory` bean from the app, but its own timeouts (see above).
- `RestClientUserAuthorizationClient`, `SourceApplicationAuthorizationClient`, `SourceApplicationAuthorization` and the
  external API auto-configuration are `internal`. `SourceApplicationJwtConverter` can be mocked, but not created outside
  the library. `InternalClientApiConfiguration` stays `public`, so tests can load it with `@ImportAutoConfiguration`.
- The library is compiled for Java 21.

## Running locally

Requires Java 25+. The Gradle wrapper is included.

```shell
./gradlew clean build         # compile and run all tests
./gradlew test                # unit tests only
./gradlew publishToMavenLocal # install locally so other apps can depend on it during development
```

To try it in another Spring Boot service, add `no.novari:flyt-web-resource-server` as a dependency, configure client
credentials for `authorization-service`, and enable the API segment you need:

```shell
SPRING_APPLICATION_JSON='{
  "novari":{
    "flyt":{
      "web-resource-server":{
        "security":{
          "api":{
            "internal":{
              "enabled":true,
              "authorized-org-id-role-pairs-json":"{\"vtfk.no\":[\"USER\",\"ADMIN\"]}"
            }
          }
        }
      }
    }
  }
}' ./gradlew bootRun
```

## Deployment

- Artifacts are published to `https://repo.fintlabs.no/releases` with `./gradlew publish`, using the Gradle publishing
  credentials (`REPOSILITE_USERNAME/PASSWORD`).
- Apps pull the library as a Maven dependency. Spring Boot finds the auto-configurations through
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Coordinate releases with the apps that use it, so they can bump the version and adjust their OAuth2 settings.

## Security

- Spring Security's servlet resource server validates the issuer against `https://idp.felleskomponent.no`.
- Path segmentation keeps `/api/intern*` internal, while `/api/**` is limited to source applications that
  authorization-service knows.
- `AuthorityMappingService` and `AuthorityPrefix` define how authorities are encoded and later parsed by host services.
- Internal role evaluation filters token roles per organisation and expands implied roles (Developer → Admin → User).

## Observability

- Health and readiness probes come from Spring Boot Actuator (`/actuator/health`, `/actuator/readiness`,
  `/actuator/prometheus` when enabled by the host).
- OAuth2 client calls and errors from authorization-service are logged by the host service. Authorization needs a new
  successful lookup in authorization-service.

## Development tips

- `authorized-org-id-role-pairs-json` must be valid JSON. Parse errors are logged at startup.
- Use `UserAuthorizationService` in host services to protect handlers the same way the filter chains do.
- Integration tests can mock `UserAuthorizationClient` to verify `403` and `503` without HTTP.

## Dependency locking

The build uses [Gradle dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html), so every
build resolves exactly the same direct and transitive versions. The locked versions are in the committed
`gradle.lockfile` files, so a PR shows every transitive version change it brings. Gradle plugins are not locked,
because Dependabot only updates `gradle.lockfile`. Versions for plugins, ktlint and Spring Boot BOM overrides are in
the version catalog `gradle/libs.versions.toml`. The exception is constraints on the plugin classpath in `buildscript`,
as in fint-flyt-kafka.

**Automatic (Dependabot):** Dependabot updates the lockfiles in its own PRs, so no manual steps are needed. If two
Dependabot PRs conflict in a lockfile, Dependabot rebases the remaining one after the first is merged. If it does not,
comment `@dependabot recreate` on the PR.

**Manual:** Any change by hand that affects dependency resolution (versions, `extra[...]` properties, constraints, added
or removed dependencies) requires regenerating the lockfiles:

```bash
./gradlew dependencies --write-locks
```

Commit the updated lockfiles with the change. Otherwise the build fails locally and in CI with an error saying the
resolved version "is not part of the dependency lock state".

## Locked API

The public API is locked with an ABI dump in `api/flyt-web-resource-server.api`, made by
[binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator). fint-flyt-gateway-starter is
compiled against one version, while the app may pull in a newer one. A changed signature then only fails at runtime,
with `NoSuchMethodError`.

- `./gradlew check` fails when the compiled API differs from the dump. After an intended change, run
  `./gradlew apiDump` and commit the dump in the same PR. On a rebase conflict in the dump, run `apiDump` again.
- Removed or changed lines in the dump require a new major. New lines require a new minor.
- `explicitApi()` is on. Keep what apps do not use `internal`.

## Contributing

1. Create a topic branch.
2. Run `./gradlew check` before opening a PR.
3. Coordinate dependency upgrades with the apps that use the library, and document new configuration keys.
4. Add or adjust unit tests when changing converters or mapping services.
