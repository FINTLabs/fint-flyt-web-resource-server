# FINT Flyt Web Resource Server

Auto-konfigurasjon for Spring Boot (Servlet-stack) som standardiserer OAuth2 resource-server-oppførsel for FINT Flyt-
tjenester. Den setter opp dedikerte `SecurityFilterChain`-er for hvert interne/eksterne API-segment, beholder rolle- og
claim-håndtering for JWT-er, og lar vertstjenester kontrollere brukerrettigheter mot authorization-service over HTTP.

## Høydepunkter

- **Segmenterte servlet filter chains** — `SecurityConfiguration` setter opp dedikerte `SecurityFilterChain`-er per
  API-segment (actuator, internal admin/user, internal client, external) og bruker det delte `AuthorizationLogFilter`-et.
- **Multi-tenant policy-kontroll** — API-segmenter styres via `novari.flyt.web-resource-server.security.api.*`-
  properties, slik at hver tjeneste kan eksponere kun de overflatene organisasjonen trenger.
- **HTTP-basert brukerautorisasjon** — `UserAuthorizationService` bruker OAuth2 client credentials mot
  authorization-service for hver kontroll av `(bruker, source-application)`.
- **HTTP-basert source application-autorisasjon** — `SourceApplicationJwtConverter` slår opp klient-ID-en hos
  authorization-service og cacher treff i ett minutt.
- **Klar for observability** — Leveres med Spring Boot Actuator aktivert slik at hver vertstjeneste arver health- og
  metrics-probes under `/actuator/**`.

## Arkitekturoversikt

| Komponent                              | Ansvar                                                                                                                   |
|----------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `SecurityConfiguration`                | Setter opp servlet filter chains for actuator, admin, user, internal-client og external med riktige matchers.            |
| `SecurityFilterChainFactoryService`    | Anvender tverrgående servlet-sikkerhetskonfigurasjon (logging, CSRF av, JWT-converter-oppkobling, deny/permit-hjelpere). |
| `SourceApplicationJwtConverter`        | Konverterer eksterne klient-JWT-er til authorities via `SourceApplicationAuthorizationClient`.                           |
| `InternalClientJwtConverter`           | Mapper interne klient-subject-claims til `CLIENT_ID_*`-authorities brukt av internal-client filter chain.                |
| `UserJwtConverter`                     | Beriker brukertokens med org-filtrerte roller; source-application-tilgang hentes ikke ved JWT-konvertering.              |
| `SourceApplicationAuthorizationClient` | Henter source application for en klient-ID fra authorization-service og cacher treff i ett minutt.                       |
| `UserAuthorizationService`             | Verktøy brukt av konsumenter for rolle- og source-application-sjekker ved kjøretid.                                      |
| `RestClientUserAuthorizationClient`    | Utfører HTTP-kall mot authorization-service for hver brukerautorisasjonskontroll.                                        |

## HTTP API

Basispath-er beskyttet av starteren (se `UrlPaths`):

| Metode | Path                    | Beskrivelse                                                                                        | Request body                       | Response                                                                |
|--------|-------------------------|----------------------------------------------------------------------------------------------------|------------------------------------|-------------------------------------------------------------------------|
| `ANY`  | `/api/intern/admin/**`  | Internal admin API som krever minst `ADMIN`-rollen; deaktivert med mindre `internal.enabled=true`. | – (implementert av vertstjenesten) | Beskyttet nedstrøms response, `401/403` når JWT-en mangler authorities. |
| `ANY`  | `/api/intern/**`        | Internal user API som krever `USER`-rollen; filtrerer tillatte roller per organisasjon.            | –                                  | Samme som over.                                                         |
| `ANY`  | `/api/intern-klient/**` | Internal client API sikret av spesifikke klient-ID-er innebygd i JWT-subject.                      | –                                  | Samme som over.                                                         |
| `ANY`  | `/api/**`               | External API for source applications som authorization-service kjenner klient-ID-en til.           | –                                  | Samme som over.                                                         |

Tokens forventes å inneholde claims under; starteren ekstraherer disse for å beregne tildelte authorities:

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

Feil faller tilbake til Spring Security sine standarder: manglende/ugyldige tokens resulterer i `401 Unauthorized`,
mens avviste authorities svarer med `403 Forbidden`.

## Autorisasjon over HTTP

Brukerautorisasjon utføres med `client_credentials` mot authorization-service-endepunktet
`POST /api/intern-klient/authorization/users/actions/authorize-source-applications`. Vertstjenester sender et
objektidentifier og et kandidatsett av source-application-ID-er; svaret er tillatelsesinterseksjonen. En enkeltsjekk
gir `403`, mens feil eller utilgjengelighet hos authorization-service gir `503`.

Source application-autorisasjon går mot `GET /api/intern-klient/authorization/clients/{clientId}` i hvert kall til
`/api/**`. Treff caches i ett minutt. `404` caches ikke og gir `401`. Andre feil caches heller ikke, og kallet avvises.

## Oppsett for apper med external API

En app med `novari.flyt.web-resource-server.security.api.external.enabled=true` trenger OAuth-klienten
`fint-flyt-authorization-oauth2-client` og registreringen `authorization-service`. Uten registreringen feiler appen ved
oppstart.

Legg secreten inn med `env`, ikke `envFrom`. Den gir `fint.sso.*`, og det kolliderer med andre OAuth-klienter i podden,
for eksempel `fint-flyt-file-oauth2-client` i gatewayene:

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

I `ra-no` heter nøklene i secreten `fint.flyt.authorization.sso.client-id` og `fint.flyt.authorization.sso.client-secret`,
så overlayet må patche `secretKeyRef.key`, slik intgr-conf-service gjør.

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

Kallene mot authorization-service og tokenhentingen fra NAM har 2s connect- og 5s read-timeout. Setter appen
`spring.http.client.connect-timeout` eller `spring.http.client.read-timeout`, gjelder de i stedet.

## Konfigurasjon

Sentrale properties eksponert av starteren:

| Property                                                                                  | Beskrivelse                                                                                                                 |
|-------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| `novari.flyt.web-resource-server.security.api.internal.enabled`                           | Aktiverer internal admin/user API-ene. User-authorization-beans opprettes når OAuth2-klientregistreringen er tilgjengelig.  |
| `novari.flyt.web-resource-server.security.api.internal.authorized-org-id-role-pairs-json` | JSON-map av `{ "orgId": ["USER","ADMIN"] }` som filtrerer tillatte roller per organisasjon.                                 |
| `novari.flyt.web-resource-server.security.api.internal-client.enabled`                    | Slår på internal client API filter chain.                                                                                   |
| `novari.flyt.web-resource-server.security.api.internal-client.authorized-client-ids`      | Liste over JWT-subjects som kan kalle `/api/intern-klient/**`.                                                              |
| `novari.flyt.web-resource-server.security.api.external.enabled`                           | Slår på external API filter chain.                                                                                          |
| `novari.flyt.web-resource-server.security.api.external.authorized-source-application-ids` | Liste over source-application-ID-er autorisert for `/api/**`.                                                               |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri`                                    | Issuer for JWT-validering (`https://idp.felleskomponent.no/nidp/oauth/nam`).                                                |
| `novari.flyt.web-resource-server.security.authorization.base-url`                         | Base-URL for authorization-service (standard `http://fint-flyt-authorization-service:8080${server.servlet.context-path:}`). |
| `novari.flyt.web-resource-server.security.authorization.client-registration-id`           | OAuth2 client registration (standard `authorization-service`).                                                              |

Tjenester som bruker `UserAuthorizationService` eller external API må levere OAuth2 client-credentials for registreringen
`authorization-service`.

## Oppgradere fra 4.x

- `no.novari:kafka` og `spring-kafka` følger ikke lenger med transitivt. Apper som bruker Kafka, må ha dem direkte.
- `SourceApplicationAuthorizationRequestService` er fjernet. Tester kan mocke `SourceApplicationAuthorizationClient`.
- `SourceApplicationJwtConverter` opprettes bare når external API er på.
- `userAuthorizationRestClient` heter nå `authorizationServiceRestClient`. Den bruker ikke lenger en
  `ClientHttpRequestFactory`-bean fra appen, men egne timeouts (se over).
- `RestClientUserAuthorizationClient`, `SourceApplicationAuthorizationClient`, `SourceApplicationAuthorization` og
  auto-konfigurasjonene for external og internal client API er `internal`. `SourceApplicationJwtConverter` kan mockes,
  men ikke opprettes utenfor biblioteket.
- Biblioteket er kompilert for Java 21.

## Kjøre lokalt

Forutsetninger:

- Java 25+
- Gradle (wrapper inkludert)

Nyttige kommandoer:

```shell
./gradlew clean build         # kompiler og kjør hele testsuiten
./gradlew test                # kun unit tests
./gradlew publishToMavenLocal # installer starteren lokalt slik at andre apper kan avhenge av den under utvikling
```

For å eksperimentere med starteren inne i en annen Spring Boot-tjeneste, legg til `no.novari:flyt-web-resource-server`
som en dependency, konfigurer OAuth2 client-credentials for `authorization-service`, og aktiver ønsket API-segment.

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

- Artefakter publiseres til `https://repo.fintlabs.no/releases` via `./gradlew publish`, og gjenbruker Gradle sine
  publiseringscredentials (`REPOSILITE_USERNAME/PASSWORD`).
- Konsumenter henter starteren som en Maven-dependency; Spring Boot oppdager automatisk auto-konfigurasjonene gjennom
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Koordiner releaser med nedstrøms tjenester slik at de kan oppgradere dependency-versjonen og tilpasse OAuth2-
  innstillinger.

## Sikkerhet

- Konfigurerer Spring Security sin servlet resource server med issuer-validering mot `https://idp.felleskomponent.no`.
- Path-nivå-segmentering sikrer at `/api/intern*`-endepunkter forblir interne mens `/api/**` er begrenset til
  autoriserte source-applications slått opp hos authorization-service.
- `AuthorityMappingService` og `AuthorityPrefix` standardiserer hvordan authorities kodes og senere parses av
  konsumerende tjenester.
- `AuthorizationLogFilter` sporer Authorization-headere (på TRACE-nivå) for å hjelpe med å diagnostisere token-
  problemer uten å lekke dem på høyere loggnivåer.
- Intern rolleevaluering filtrerer token-roller per organisasjon og utvider implisitte roller (Developer → Admin → User).

## Observability og drift

- Health- og readiness-probes arves gjennom Spring Boot Actuator (`/actuator/health`, `/actuator/readiness`,
  `/actuator/prometheus` når aktivert av vertsappen).
- OAuth2-klientkall og feil fra authorization-service logges av vertstjenesten. Autorisasjon krever et nytt, vellykket
  oppslag hos authorization-service.

## Utviklingstips

- Når `authorized-org-id-role-pairs-json` oppdateres, sørg for at payloaden er gyldig JSON; starteren logger parse-
  feil ved oppstart.
- Bruk `UserAuthorizationService` inne i vertstjenester for å beskytte handlers på samme måte som filter chains gjør.
- Integrasjonstester kan mocke `UserAuthorizationClient` for å verifisere `403` og `503` uten HTTP.

## Låsing av avhengigheter

Bygget bruker [Gradle dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html), slik at hvert bygg løser opp nøyaktig de samme direkte og transitive versjonene. De låste versjonene ligger i de committede `gradle.lockfile`-filene. En PR viser derfor alle transitive versjonsendringer den fører med seg. Gradle-plugins låses ikke, fordi Dependabot bare oppdaterer `gradle.lockfile`; versjonene deres er i stedet låst i `plugins`-blokka.

**Automatisk (Dependabot):** Dependabot oppdaterer lockfilene i sine egne PR-er, så det trengs ingen manuelle steg. Hvis to Dependabot-PR-er kommer i konflikt i en lockfile, rebaser Dependabot den gjenværende etter at den første er merget. Skjer ikke det, kommenter `@dependabot recreate` på PR-en.

**Manuelt:** Alle endringer du gjør for hånd som påvirker oppløsningen av avhengigheter, krever at lockfilene regenereres. Det gjelder versjoner, `extra[...]`-properties, constraints og nye eller fjernede avhengigheter:

```bash
./gradlew dependencies --write-locks
```

Commit de oppdaterte lockfilene sammen med endringen. Glemmer du det, feiler bygget både lokalt og i CI med en feilmelding om at den oppløste versjonen «is not part of the dependency lock state».

## Låst API

Det offentlige API-et er låst med en ABI-dump i `api/flyt-web-resource-server.api`, laget av
[binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator). fint-flyt-gateway-starter
er kompilert mot én versjon, mens appen kan dra inn en nyere. En endret signatur feiler da først ved kjøring, med
`NoSuchMethodError`.

- `./gradlew check` feiler når det kompilerte API-et avviker fra dumpen. Etter en tilsiktet endring: kjør
  `./gradlew apiDump` og commit dumpen i samme PR. Ved rebase-konflikt i dumpen: kjør `apiDump` på nytt.
- Fjernede eller endrede linjer i dumpen krever ny major. Nye linjer krever ny minor.
- `explicitApi()` er på. Hold det appene ikke bruker `internal`.

## Bidra

1. Opprett en topic-branch.
2. Kjør `./gradlew test` (eller `./gradlew clean build`) før du åpner en PR.
3. Koordiner dependency-versjonsoppgraderinger med konsumenter og dokumenter eventuelle nye konfigurasjonsnøkler.
4. Legg til eller juster unit tests når du endrer converters eller mapping services.

———

FINT Flyt Web Resource Server vedlikeholdes av FINT Flyt-teamet. Ta kontakt på den interne Slack-kanalen eller opprett
en issue i dette repositoriet for spørsmål eller forbedringer.
