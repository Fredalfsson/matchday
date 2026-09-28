# Matchday

Matchday är en social webbapplikation med matcher i Allsvenskan som utgångspunkt. Projektet
innehåller en Spring Boot-backend, en Next.js-frontend samt automatiserade kvalitets- och
säkerhetskontroller.

Backend stöder manuell import och uppdatering av matchdata från TheSportsDB, ett publikt API för
matcher som har sparats i PostgreSQL samt ett API där en autentiserad användare med en
applikationsidentitet kan skapa en diskussionsgrupp (`circle`) för en match.

## Teknik

| Område | Teknik |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1.1, Maven |
| Databas | PostgreSQL 18, Flyway |
| Frontend | Next.js, TypeScript, Tailwind CSS |
| Testning | JUnit, Spring Boot Test, Testcontainers, JaCoCo |
| CI och säkerhet | GitHub Actions, Redocly CLI, OWASP Dependency-Check |

## Backendarkitektur

Backend använder en feature-baserad paketstruktur med separata lager för API, applikationslogik,
domän och infrastruktur. PostgreSQL är primär datakälla. TheSportsDB-integrationen implementerar
det interna gränssnittet `MatchDataProvider`, vilket håller leverantörens datamodell utanför
domänmodellen och det publika API-kontraktet.

Stödet för circles följer samma struktur. Gränssnittet `CurrentUser` skiljer applikationslogiken
från den ännu ofärdiga autentiseringsintegrationen.

## Projektstruktur

```text
matchday/
├── .github/workflows/    CI- och säkerhetskontroller
├── backend/              Spring Boot-applikation
├── docs/                 API- och processdokumentation
├── frontend/             Next.js-applikation
├── compose.yaml          PostgreSQL för lokal utveckling
└── README.md
```

## Lokal utveckling

### Förutsättningar

- JDK 25
- Docker med Docker Compose
- Git
- Node.js 24 för att köra OpenAPI-kontrollen lokalt

Maven behöver inte installeras separat eftersom projektet innehåller Maven Wrapper.

### Starta backend

```bash
git clone https://github.com/Fredalfsson/matchday.git
cd matchday/backend
./mvnw spring-boot:run
```

I Windows PowerShell används `.\mvnw.cmd spring-boot:run` i stället för `./mvnw`.

Spring Boot använder `compose.yaml` i projektroten för att starta PostgreSQL och stoppar tjänsten
när backendprocessen avslutas.

PostgreSQL använder lokala standardvärden för databas, användare, lösenord och port. Värdena kan
ersättas med miljövariablerna `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` och
`POSTGRES_PORT`. Databasinnehållet sparas i Docker-volymen `matchday_postgres_data`.

Låt terminalen vara öppen medan backend körs. När loggen visar att applikationen har startat,
öppna en ny terminal och kontrollera den publika matchlistan:

```bash
curl --fail-with-body http://127.0.0.1:8080/api/v1/matches
```

Svaret är `[]` om databasen ännu inte innehåller några matcher. Fortsätt då med avsnittet
[Manuell matchimport lokalt](#manuell-matchimport-lokalt).

## API

| Metod och sökväg | Åtkomst | Beskrivning |
| --- | --- | --- |
| `GET /api/v1/matches` | Publik | Returnerar sparade matcher |
| `POST /api/v1/matches/{matchId}/circle` | Autentiserad med applikationsidentitet | Skapar matchens circle och aktiverar skaparens medlemskap |
| `POST /api/v1/admin/match-imports` | Rollen `MATCH_IMPORTER` | Importerar eller uppdaterar angiven säsong |

Matchlistan sorteras efter säsong, omgång, datum, avsparkstid och internt match-id. Frontend
ansvarar för filtrering och uppdelning mellan kommande och spelade matcher.

API-kontraktet finns i [`docs/openapi.yaml`](docs/openapi.yaml) och följer OpenAPI 3.1.0.

### Circle-stöd i nuvarande backend

- En match kan ha noll eller en circle.
- Circle skapas först när en autentiserad användare med en tillgänglig applikationsidentitet
  startar diskussionen.
- Skaparen blir medlem i samma transaktion som circle skapas.
- Svaret exponerar circle-ID, match-ID, skapandetid och aktiv medlemsstatus, men inte internt
  användar-ID eller e-postadress.
- Samtidiga skapandeförsök skyddas av databasens unika villkor. Ett anrop får `201 Created` och
  övriga får `409 Conflict`.

API för att läsa en circle, ansluta till eller lämna den samt skicka meddelanden är ännu inte
implementerade. Den lokala Basic Auth-profilen autentiserar administrativa importanrop men kopplar
inte inloggningen till ett användar-ID i domänen. Circle-anrop kräver därför auth-modulens framtida
`CurrentUser`-adapter. Backendens automatiserade tester använder en avgränsad testadapter så att
arbetet med circles inte blockeras av auth-utvecklingen.

## Manuell matchimport lokalt

Profilen `local` konfigurerar Basic Auth med rollen `MATCH_IMPORTER` och binder servern till
`127.0.0.1`. Stoppa en redan startad backendprocess och starta backend med lokala
inloggningsuppgifter:

```bash
SPRING_PROFILES_ACTIVE=local \
MATCHDAY_LOCAL_USERNAME=local-operator \
MATCHDAY_LOCAL_PASSWORD=choose-a-local-password \
./mvnw spring-boot:run
```

Kör importen från en annan terminal. `curl` frågar efter lösenordet för `local-operator`:

```bash
curl --fail-with-body \
  --user local-operator \
  --header 'Content-Type: application/json' \
  --data '{"season": 2026}' \
  http://127.0.0.1:8080/api/v1/admin/match-imports
```

Importen hämtar 30 omgångar sekventiellt och kan ta över en minut. `curl` visar inget medan
anropet pågår utan skriver svaret först när importen är klar. Om TheSportsDB svarar med `429 Too
Many Requests` väntar backend enligt `Retry-After` innan den försöker igen, vilket kan förlänga
körtiden. Återförsök och eventuella fel visas i terminalen där backend körs.

Alla 240 matcher finns inte nödvändigtvis hos providern när säsongens spelschema börjar
fastställas. Importen sparar därför de matcher som finns och kan köras igen för att fylla på nya
matcher eller uppdatera redan lagrade matcher. Tomma och ofullständiga omgångar är tillåtna och
raderar aldrig befintliga data. Backend avvisar däremot matcher för fel säsong eller omgång,
duplicerade externa match-ID:n och fler än åtta matcher i samma omgång.

Integrationen använder TheSportsDB v1 och standardnyckeln `123`. En annan nyckel anges med
miljövariabeln `THESPORTSDB_API_KEY`.

## Verifiering

Kör backendens lokala kvalitetskontroller med:

```bash
cd backend
./mvnw --batch-mode --no-transfer-progress verify
```

Kommandot kör tester, kontrollerar kodformat, genererar en JaCoCo-rapport, verifierar minst 70
procent linjetäckning och bygger applikationen. GitHub Actions kör samma backendverifiering och
separata kontroller för OpenAPI-kontraktet och Maven-beroenden.

Kör samma OpenAPI-kontroll som CI från projektets rotkatalog med:

```bash
REDOCLY_TELEMETRY=off \
REDOCLY_SUPPRESS_UPDATE_NOTICE=true \
npx --yes @redocly/cli@2.45.0 lint docs/openapi.yaml
```

## Dokumentation

- [OpenAPI-kontrakt](docs/openapi.yaml)
- [Backend CI och säkerhetskontroller](docs/BACKEND_CI.md)
- [Frontend](frontend/README.md)
