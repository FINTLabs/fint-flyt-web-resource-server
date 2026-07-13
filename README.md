# FINT Flyt Web Resource Server

Auto-konfigurasjon for Spring Boot (Servlet-stack) som standardiserer OAuth2 resource-server-oppførsel for FINT Flyt-
tjenester. Den setter opp dedikerte `SecurityFilterChain`-er for hvert interne/eksterne API-segment, beriker JWT-er med
Kafka-baserte rettigheter, og cacher utledede authorities slik at nedstrøms applikasjoner kan beskytte HTTP-endepunkter
med konsistent semantikk.

## Høydepunkter

- **Segmenterte servlet filter chains** — `SecurityConfiguration` setter opp dedikerte `SecurityFilterChain`-er per
  API-segment (actuator, internal admin/user, internal client, external) og bruker det delte `AuthorizationLogFilter`-et.
- **Multi-tenant policy-kontroll** — API-segmenter styres via `novari.flyt.web-resource-server.security.api.*`-
  properties, slik at hver tjeneste kan eksponere kun de overflatene organisasjonen trenger.
- **Kafka-basert autorisasjon** — `SourceApplicationAuthorizationRequestService` utfører request/reply-oppslag for
  source-application-ID-er, mens `UserPermissionCachingListenerFactory` strømmer brukerrettigheter inn i cache.
- **FINT cache-integrasjon** — Langlevde rettighetsdata lagres i en `FintCache` for å unngå gjentatte Kafka round-trips
  og for å øke hastigheten på rollesjekker.
- **Klar for observability** — Leveres med Spring Boot Actuator aktivert slik at hver vertstjeneste arver health- og
  metrics-probes under `/actuator/**`.

## Arkitekturoversikt

| Komponent                                   | Ansvar                                                                                                             |
|---------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `SecurityConfiguration`                     | Setter opp servlet filter chains for actuator, admin, user, internal-client og external med riktige matchers.      |
| `SecurityFilterChainFactoryService`         | Anvender tverrgående servlet-sikkerhetskonfigurasjon (logging, CSRF av, JWT-converter-oppkobling, deny/permit-hjelpere). |
| `SourceApplicationJwtConverter`             | Konverterer eksterne klient-JWT-er til authorities ved å be om autorisasjonsdetaljer over Kafka.                   |
| `InternalClientJwtConverter`                | Mapper interne klient-subject-claims til `CLIENT_ID_*`-authorities brukt av internal-client filter chain.          |
| `UserJwtConverter`                          | Beriker brukertokens med org-filtrerte roller og cachede source-application-ID-er hentet fra Kafka.                |
| `UserPermissionCachingListenerFactory`      | Bygger en Kafka-listener som holder `FintCache<UUID, UserPermission>` oppdatert for JWT-converteren.                |
| `SourceApplicationAuthorizationRequestService` | Håndterer Kafka request/reply-infrastruktur som løser klient-ID-er til source-application-ID-er.                |
| `UserAuthorizationService`                  | Verktøy brukt av konsumenter av denne starteren for å verifisere rollemedlemskap eller applikasjonsnivå-tilgang ved kjøretid. |

## HTTP API

Basispath-er beskyttet av starteren (se `UrlPaths`):

| Metode | Path                     | Beskrivelse                                                                                       | Request body                                   | Response                                                                 |
|--------|--------------------------|----------------------------------------------------------------------------------------------------|------------------------------------------------|--------------------------------------------------------------------------|
| `ANY`  | `/api/intern/admin/**`   | Internal admin API som krever minst `ADMIN`-rollen; deaktivert med mindre `internal.enabled=true`.  | – (implementert av vertstjenesten)             | Beskyttet nedstrøms response, `401/403` når JWT-en mangler authorities.   |
| `ANY`  | `/api/intern/**`         | Internal user API som krever `USER`-rollen; filtrerer tillatte roller per organisasjon.             | –                                              | Samme som over.                                                          |
| `ANY`  | `/api/intern-klient/**`  | Internal client API sikret av spesifikke klient-ID-er innebygd i JWT-subject.                       | –                                              | Samme som over.                                                          |
| `ANY`  | `/api/**`                | External API for godkjente source-applications annonsert via Kafka.                                 | –                                              | Samme som over.                                                          |

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

## Kafka-integrasjon

- `SourceApplicationAuthorizationRequestService` oppretter authorization-request-topics med standard org/application-
  prefiks, starter kortlevde reply-topics (2 minutters retention), og bruker `RequestTemplate` til å utføre
  request/reply-kall som oversetter klient-ID-er til source-application-ID-er.
- `UserPermissionCachingListenerFactory` abonnerer på `userpermission`-entity-topicen via
  `ParameterizedListenerContainerFactoryService`, skriver records inn i den delte `FintCache`-en, og hopper over
  feilede records gjennom en `ErrorHandlerFactory`.
- Kafka-tilkobling, group-ID-er og polling-parametere avhenger av de delte `no.novari:kafka`-hjelperne slik at
  starteren arver FINT-standardene (max poll-innstillinger, seek-to-beginning bootstrap, osv.).

## Planlagte oppgaver

Ingen scheduled jobs er definert; levetiden til rettigheter håndteres utelukkende gjennom Kafka-listeners og den
konfigurerte cache-TTL-en (`novari.cache.default-cache-entry-time-to-live`), så ingen cron-lignende opprydding er
nødvendig.

## Konfigurasjon

Sentrale properties eksponert av starteren:

| Property                                                            | Beskrivelse                                                                                      |
|-----------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| `novari.flyt.web-resource-server.security.api.internal.enabled`         | Aktiverer internal admin/user API-ene og registrerer de Kafka-baserte user-authorization-beans-ene. |
| `novari.flyt.web-resource-server.security.api.internal.authorized-org-id-role-pairs-json` | JSON-map av `{ "orgId": ["USER","ADMIN"] }` som filtrerer tillatte roller per organisasjon. |
| `novari.flyt.web-resource-server.security.api.internal-client.enabled`  | Slår på internal client API filter chain.                                                          |
| `novari.flyt.web-resource-server.security.api.internal-client.authorized-client-ids` | Liste over JWT-subjects som kan kalle `/api/intern-klient/**`.                        |
| `novari.flyt.web-resource-server.security.api.external.enabled`         | Slår på external API filter chain.                                                                 |
| `novari.flyt.web-resource-server.security.api.external.authorized-source-application-ids` | Liste over source-application-ID-er autorisert for `/api/**`.                        |
| `novari.kafka.application-id`                                        | Brukes til navngiving av request/reply-topics og listener group-ID-er.                             |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri`               | Issuer for JWT-validering (`https://idp.felleskomponent.no/nidp/oauth/nam`).                       |
| `novari.cache.default-cache-entry-time-to-live`                      | Standard cache-TTL (10 år som standard) for cachede rettighetsoppføringer.                          |

Secrets levert via den konsumerende tjenestens deployment (Kubernetes secret, Vault, osv.) må inneholde OAuth-
klientdata, Kafka bootstrap-credentials, og eventuelle organisasjonsspesifikke JSON-payloads nevnt over.

## Kjøre lokalt

Forutsetninger:

- Java 25+
- Gradle (wrapper inkludert)
- Kafka broker (for å kjøre permission-listeneren/request-reply-interaksjonene)

Nyttige kommandoer:

```shell
./gradlew clean build         # kompiler og kjør hele testsuiten
./gradlew test                # kun unit tests
./gradlew publishToMavenLocal # installer starteren lokalt slik at andre apper kan avhenge av den under utvikling
```

For å eksperimentere med starteren inne i en annen Spring Boot-tjeneste, legg til `no.novari:fint-flyt-web-resource-server`
som en dependency, kjør en lokal Kafka broker (f.eks. docker-compose), og aktiver ønsket API-segment:

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

- Artefakter publiseres til `https://repo.fintlabs.no/releases` via `./gradlew publish`, og gjenbruker Gradle sine
  publiseringscredentials (`REPOSILITE_USERNAME/PASSWORD`).
- Konsumenter henter starteren som en Maven-dependency; Spring Boot oppdager automatisk auto-konfigurasjonene gjennom
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Koordiner releaser med nedstrøms tjenester slik at de kan oppgradere dependency-versjonen og tilpasse Kafka/OAuth-
  innstillinger.

## Sikkerhet

- Konfigurerer Spring Security sin servlet resource server med issuer-validering mot `https://idp.felleskomponent.no`.
- Path-nivå-segmentering sikrer at `/api/intern*`-endepunkter forblir interne mens `/api/**` er begrenset til
  autoriserte source-applications utledet fra Kafka.
- `AuthorityMappingService` og `AuthorityPrefix` standardiserer hvordan authorities kodes og senere parses av
  konsumerende tjenester.
- `AuthorizationLogFilter` sporer Authorization-headere (på TRACE-nivå) for å hjelpe med å diagnostisere token-
  problemer uten å lekke dem på høyere loggnivåer.
- Intern rolleevaluering filtrerer token-roller per organisasjon og utvider implisitte roller (Developer → Admin → User).

## Observability og drift

- Health- og readiness-probes arves gjennom Spring Boot Actuator (`/actuator/health`, `/actuator/readiness`,
  `/actuator/prometheus` når aktivert av vertsappen).
- Cache-tilstand kan inspiseres via `FintCacheManager`-metrics når Micrometer er aktivert i den konsumerende
  tjenesten.
- Kafka-listeners logger konsumerte permission-events på DEBUG slik at operatører kan spore autorisasjonsendringer
  ved behov.

## Utviklingstips

- Når `authorized-org-id-role-pairs-json` oppdateres, sørg for at payloaden er gyldig JSON; starteren logger parse-
  feil ved oppstart.
- Bruk `UserAuthorizationService` inne i vertstjenester for å beskytte handlers på samme måte som filter chains gjør.
- Integrasjonstester kan mocke `ReplyTopicService` og `RequestTemplateFactory` for å unngå Kafka mens JWT-converterne
  fortsatt dekkes.
- `UserPermissionCachingListenerFactory` gjenbruker standard Kafka-innstillinger; overstyr listener-konfigurasjonen
  kun hvis du virkelig trenger andre poll-størrelser eller retry-logikk.

## Bidra

1. Opprett en topic-branch.
2. Kjør `./gradlew test` (eller `./gradlew clean build`) før du åpner en PR.
3. Koordiner dependency-versjonsoppgraderinger med konsumenter og dokumenter eventuelle nye konfigurasjonsnøkler.
4. Legg til eller juster unit tests når du endrer converters, mapping services, eller Kafka-listeners.

———

FINT Flyt Web Resource Server vedlikeholdes av FINT Flyt-teamet. Ta kontakt på den interne Slack-kanalen eller opprett
en issue i dette repositoriet for spørsmål eller forbedringer.
