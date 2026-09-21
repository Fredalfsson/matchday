# Backend CI och säkerhetskontroller

Detta dokument beskriver backendens automatiserade kvalitets- och säkerhetskontroller. Målgruppen
är utvecklare som arbetar i kodbasen samt granskare som behöver bedöma hur ändringar verifieras
innan de integreras i `main`.

## Översikt

Backend använder två separata GitHub Actions-arbetsflöden:

| Arbetsflöde | Syfte | Utlösare |
| --- | --- | --- |
| `Backend CI` | Bygg, tester, kodformat, testtäckning och OpenAPI-kontrakt | Pull request mot `main`, push till `main`, manuell körning |
| `Backend Security` | Sårbarhetsanalys av Maven-beroenden | Pull request mot `main`, push till `main`, måndagar 06.00 UTC, manuell körning |

Sårbarhetsanalysen är separerad från det ordinarie bygget eftersom den kräver åtkomst till NVD och
en API-nyckel. Resultaten visas därför som separata status checks för kodverifiering,
API-kontrakt och beroenden.

## Backend CI

Konfiguration: [`.github/workflows/backend.yml`](../.github/workflows/backend.yml)

Arbetsflödet innehåller två oberoende jobb. Ett fel i API-kontraktet kan därför skiljas från ett
kompilerings- eller testfel.

### Backend verification

Jobbet använder Temurin JDK 25 och kör följande kommando från `backend/`:

```bash
./mvnw --batch-mode --no-transfer-progress verify
```

Verifieringen omfattar:

- kompilering och paketering
- enhets- och integrationstester
- kodformat med Spotless och Google Java Format
- JaCoCo-rapport och en kvalitetsgrind på minst 70 procent linjetäckning
- Testcontainers-baserade integrationstester mot PostgreSQL

Följande rapporter laddas upp som GitHub Actions-artifacts även när jobbet misslyckas:

- `backend/target/surefire-reports/`
- `backend/target/site/jacoco/`

Rapporterna behålls i sju dagar.

### OpenAPI lint

Jobbet validerar [`docs/openapi.yaml`](openapi.yaml) med Redocly CLI. Kontrollen omfattar giltig
OpenAPI-struktur, schema-referenser och Redoclys rekommenderade kontraktsregler. Redocly används
endast i utvecklings- och CI-flödet och påverkar inte Spring Boot-applikationens runtime-beroenden.

Arbetsflödet använder Node.js 24 och en exakt version av Redocly CLI för att begränsa variationen
mellan körningar. Den lokala motsvarigheten körs från projektets rotkatalog:

```bash
REDOCLY_TELEMETRY=off \
REDOCLY_SUPPRESS_UPDATE_NOTICE=true \
npx --yes @redocly/cli@2.45.0 lint docs/openapi.yaml
```

## Backend Security

Konfiguration: [`.github/workflows/backend-security.yml`](../.github/workflows/backend-security.yml)

Jobbet `Backend dependency audit` använder OWASP Dependency-Check för att analysera
Maven-beroenden mot National Vulnerability Database. Säkerhetsjobbet misslyckas när ett beroende
får CVSS 7 eller högre. Den schemalagda veckokörningen gör att nya sårbarheter kan upptäckas även om
beroendefilerna inte har ändrats.

Dependency-Check körs med tester avstängda eftersom backendens tester redan hanteras av
`Backend verification`. En HTML-rapport laddas upp som en GitHub Actions-artifact och behålls i
sju dagar:

```text
backend/target/dependency-check-report.html
```

### NVD API-nyckel

Arbetsflödet läser följande GitHub Actions repository secret:

```text
NVD_API_KEY
```

Nyckeln används av OWASP Dependency-Check för anrop till National Vulnerability Database och
lagras inte i versionshanteringen. Arbetsflödet avbryts om nyckeln saknas.

En kostnadsfri nyckel kan begäras via
[National Vulnerability Database](https://nvd.nist.gov/developers/request-an-api-key). Lägg till
den som en repository secret under **Settings > Secrets and variables > Actions** i GitHub. Namnet
ska vara exakt `NVD_API_KEY`.

NVD-data cachelagras veckovis. Om ingen kompatibel cache finns initieras databasen från
Dependency-Check-projektets datafeed innan den uppdateras mot NVD API med repositoryts
`NVD_API_KEY`. Uppdateringen och själva beroendeanalysen körs som separata steg, vilket gör att en
färdiguppdaterad databas kan cachelagras även om analysen därefter hittar en sårbarhet och stoppar
bygget. Cacheversionen följer Dependency-Checks huvudversion och ska ändras vid en
inkompatibel uppgradering.

### Exekveringskontext

Teamets arbetsflöde använder branches i samma GitHub-repository. GitHub lämnar inte ut repository
secrets till körningar från forks eller Dependabot; sådana körningar stoppas därför när
`NVD_API_KEY` verifieras. Arbetsflödet använder inte `pull_request_target`.

## Minsta behörighet och leveranskedja

Båda arbetsflödena har endast `contents: read`. Checkout-steget använder
`persist-credentials: false`, vilket förhindrar att GitHub-tokenet sparas i den lokala
Git-konfigurationen efter checkout.

Varje GitHub Action anges med ett fullständigt commit-SHA-värde. Releaseversionen står i en
kommentar vid respektive SHA för att göra versionsvalet granskningsbart.

Maven Wrapper låser Maven-versionen och verifierar den nedladdade distributionen med SHA-256.
Version och checksumma finns i `backend/.mvn/wrapper/maven-wrapper.properties`.

## Status checks

Arbetsflödena publicerar följande status checks:

- `Backend verification`
- `OpenAPI lint`
- `Backend dependency audit`

Kontrollerna redovisar backendverifiering, API-kontrakt och beroenderisker separat i varje pull
request mot `main`.
