# Matchday

Applikation för att se kommande matcher i allsvenskan, samt skapa eller gå med i grupper för att chatta med likasinnade.

## Stack
### Backend Stack

Spring Boot 4.1.1, Java 25, Maven, PostgreSQL 18, Flyway och Spring Security. Den manuella
matchimporten använder Basic Auth i den lokala utvecklingsprofilen. Projektets gemensamma
autentiseringslösning integreras separat.

### Frontend Stack

Next.js, TypeScript, Tailwind CSS.

## Struktur

```
matchday/
├── .github/workflows/    CI
├── backend/              Spring Boot-applikationen
├── frontend/             Next.js-applikationen
├── compose.yaml          Postgres för lokal utveckling
└── README.md
```



## Kom igång
### Backend
Krav: JDK 25, Docker, Docker Compose.

```bash
git clone git@github.com:Fredalfsson/matchday.git
cd matchday
docker compose up -d
cd backend
./mvnw spring-boot:run
```
För windows i PowerShell = `.\mvnw.cmd spring-boot:run`.

### Manuell matchimport lokalt

Den skyddade import-endpointen kan testas utan den kommande externa autentiseringslösningen
genom att starta backend med profilen `local`. Ange egna lokala inloggningsuppgifter via
miljövariabler; lägg dem inte i versionshanterade filer.

macOS och Linux:

```bash
SPRING_PROFILES_ACTIVE=local \
MATCHDAY_LOCAL_USERNAME=local-operator \
MATCHDAY_LOCAL_PASSWORD=choose-a-local-password \
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:MATCHDAY_LOCAL_USERNAME="local-operator"
$env:MATCHDAY_LOCAL_PASSWORD="choose-a-local-password"
.\mvnw.cmd spring-boot:run
```

Anropa sedan `POST http://127.0.0.1:8080/api/v1/admin/match-imports` med Basic Auth och
följande JSON-body:

```json
{
  "season": 2026
}
```

Profilen binder som standard servern till `127.0.0.1` och är endast avsedd för lokal
utveckling. Adressen kan vid behov ersättas med `MATCHDAY_LOCAL_SERVER_ADDRESS`.

### Hämta matcher

`GET http://127.0.0.1:8080/api/v1/matches` är publik och returnerar matcher från Matchdays
databas. Endpointen anropar alltså inte TheSportsDB vid varje läsning.

Backend returnerar matcherna i en stabil standardordning:

1. säsong
2. omgång
3. schemalagt datum
4. avsparkstid, där okänd tid placeras sist för samma datum
5. internt match-id som sista skiljekriterium

Frontend kan fortfarande filtrera eller presentera kommande och spelade matcher på det sätt
som passar gränssnittet.

### Frontend
..
