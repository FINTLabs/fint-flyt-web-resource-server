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
  authorization-service og cacher både tillatte og avviste `(bruker, source-application)`-svar i 15 sekunder.
- **Kafka for eksterne klienter** — `SourceApplicationAuthorizationRequestService` beholder request/reply-oppslag for
  klient-ID-er som eksponeres via eksterne API-er.
- **Klar for observability** — Leveres med Spring Boot Actuator aktivert slik at hver vertstjeneste arver health- og
  metrics-probes under `/actuator/**`.

## Arkitekturoversikt

| Komponent                                      | Ansvar                                                                                                                   |
|------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `SecurityConfiguration`                        | Setter opp servlet filter chains for actuator, admin, user, internal-client og external med riktige matchers.            |
| `SecurityFilterChainFactoryService`            | Anvender tverrgående servlet-sikkerhetskonfigurasjon (logging, CSRF av, JWT-converter-oppkobling, deny/permit-hjelpere). |
| `SourceApplicationJwtConverter`                | Konverterer eksterne klient-JWT-er til authorities ved å be om autorisasjonsdetaljer over Kafka.                         |
| `InternalClientJwtConverter`                   | Mapper interne klient-subject-claims til `CLIENT_ID_*`-authorities brukt av internal-client filter chain.                |
| `UserJwtConverter`                             | Beriker brukertokens med org-filtrerte roller; source-application-tilgang hentes ikke ved JWT-konvertering.              |
| `SourceApplicationAuthorizationRequestService` | Håndterer Kafka request/reply-infrastruktur som løser klient-ID-er til source-application-ID-er.                         |
| `UserAuthorizationService`                     | Verktøy brukt av konsumenter for rolle- og source-application-sjekker ved kjøretid.                                      |
| `CachingUserAuthorizationClient`               | Cacher positive og negative HTTP-svar i 15 sekunder; utløpte svar brukes aldri når authorization-service feiler.         |

## HTTP API

Basispath-er beskyttet av starteren (se `UrlPaths`):

| Metode | Path                    | Beskrivelse                                                                                        | Request body                       | Response                                                                |
|--------|-------------------------|----------------------------------------------------------------------------------------------------|------------------------------------|-------------------------------------------------------------------------|
| `ANY`  | `/api/intern/admin/**`  | Internal admin API som krever minst `ADMIN`-rollen; deaktivert med mindre `internal.enabled=true`. | – (implementert av vertstjenesten) | Beskyttet nedstrøms response, `401/403` når JWT-en mangler authorities. |
| `ANY`  | `/api/intern/**`        | Internal user API som krever `USER`-rollen; filtrerer tillatte roller per organisasjon.            | –                                  | Samme som over.                                                         |
| `ANY`  | `/api/intern-klient/**` | Internal client API sikret av spesifikke klient-ID-er innebygd i JWT-subject.                      | –                                  | Samme som over.                                                         |
| `ANY`  | `/api/**`               | External API for godkjente source-applications annonsert via Kafka.                                | –                                  | Samme som over.                                                         |

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
gir `403`, og en feil ved oppfriskning av en utløpt cache gir `503` — tidligere cacheverdier brukes ikke.

## Kafka-integrasjon

- `SourceApplicationAuthorizationRequestService` oppretter authorization-request-topics med standard org/application-
  prefiks, starter kortlevde reply-topics (2 minutters retention), og bruker `RequestTemplate` til å utføre
  request/reply-kall som oversetter klient-ID-er til source-application-ID-er.
- Kafka-tilkobling, group-ID-er og polling-parametere avhenger av de delte `no.novari:kafka`-hjelperne slik at
  starteren kan utføre request/reply for eksterne klienter.

## Planlagte oppgaver

Ingen scheduled jobs er definert. Brukerautorisasjonscache opphører automatisk etter 15 sekunder.

## Konfigurasjon

Sentrale properties eksponert av starteren:

| Property                                                                                  | Beskrivelse                                                                                                                |
|-------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `novari.flyt.web-resource-server.security.api.internal.enabled`                           | Aktiverer internal admin/user API-ene. User-authorization-beans opprettes når OAuth2-klientregistreringen er tilgjengelig. |
| `novari.flyt.web-resource-server.security.api.internal.authorized-org-id-role-pairs-json` | JSON-map av `{ "orgId": ["USER","ADMIN"] }` som filtrerer tillatte roller per organisasjon.                                |
| `novari.flyt.web-resource-server.security.api.internal-client.enabled`                    | Slår på internal client API filter chain.                                                                                  |
| `novari.flyt.web-resource-server.security.api.internal-client.authorized-client-ids`      | Liste over JWT-subjects som kan kalle `/api/intern-klient/**`.                                                             |
| `novari.flyt.web-resource-server.security.api.external.enabled`                           | Slår på external API filter chain.                                                                                         |
| `novari.flyt.web-resource-server.security.api.external.authorized-source-application-ids` | Liste over source-application-ID-er autorisert for `/api/**`.                                                              |
| `novari.kafka.application-id`                                                             | Brukes til navngiving av request/reply-topics og listener group-ID-er.                                                     |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri`                                    | Issuer for JWT-validering (`https://idp.felleskomponent.no/nidp/oauth/nam`).                                               |
| `novari.flyt.web-resource-server.security.authorization.base-url`                         | Base-URL for authorization-service (standard `http://fint-flyt-authorization-service:8080`).                               |
| `novari.flyt.web-resource-server.security.authorization.client-registration-id`           | OAuth2 client registration (standard `authorization-service`).                                                             |
| `novari.flyt.web-resource-server.security.authorization.cache.ttl`                        | TTL for positive og negative brukerrettighetssvar (standard `15s`).                                                        |
| `novari.flyt.web-resource-server.security.authorization.cache.stale-if-error`             | Tillatt stale-periode etter TTL når authorization-service ikke svarer (standard `2m`).                                     |

Tjenester som bruker `UserAuthorizationService` må levere OAuth2 client-credentials for registreringen
`authorization-service`; starteren oppretter ikke HTTP-klienten i apper uten OAuth2-klientinfrastruktur. Eventuelle
Kafka-credentials gjelder kun funksjonene som fortsatt bruker request/reply.

## Kjøre lokalt

Forutsetninger:

- Java 25+
- Gradle (wrapper inkludert)
- Kafka broker (kun nødvendig for external source-application request/reply)

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
  autoriserte source-applications utledet via Kafka request/reply.
- `AuthorityMappingService` og `AuthorityPrefix` standardiserer hvordan authorities kodes og senere parses av
  konsumerende tjenester.
- `AuthorizationLogFilter` sporer Authorization-headere (på TRACE-nivå) for å hjelpe med å diagnostisere token-
  problemer uten å lekke dem på høyere loggnivåer.
- Intern rolleevaluering filtrerer token-roller per organisasjon og utvider implisitte roller (Developer → Admin → User).

## Observability og drift

- Health- og readiness-probes arves gjennom Spring Boot Actuator (`/actuator/health`, `/actuator/readiness`,
  `/actuator/prometheus` når aktivert av vertsappen).
- OAuth2-klientkall og feil ved cacheoppfriskning logges av vertstjenesten; en utdatert cacheverdi blir aldri servert.

## Utviklingstips

- Når `authorized-org-id-role-pairs-json` oppdateres, sørg for at payloaden er gyldig JSON; starteren logger parse-
  feil ved oppstart.
- Bruk `UserAuthorizationService` inne i vertstjenester for å beskytte handlers på samme måte som filter chains gjør.
- Integrasjonstester kan mocke `UserAuthorizationClient` for å verifisere `403`, `503` og cacheoppførsel uten HTTP.

## Bidra

1. Opprett en topic-branch.
2. Kjør `./gradlew test` (eller `./gradlew clean build`) før du åpner en PR.
3. Koordiner dependency-versjonsoppgraderinger med konsumenter og dokumenter eventuelle nye konfigurasjonsnøkler.
4. Legg til eller juster unit tests når du endrer converters, mapping services, eller Kafka-listeners.

———

FINT Flyt Web Resource Server vedlikeholdes av FINT Flyt-teamet. Ta kontakt på den interne Slack-kanalen eller opprett
en issue i dette repositoriet for spørsmål eller forbedringer.
