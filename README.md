# Matchday

Fotbolls-app med gruppmeddelanden och betting. Kursprojekt i DevSecOps.

## Innehåll

- [Stack](#stack)
- [Struktur](#struktur)
- [Kom igång](#kom-igång)
- [Så ska backend byggas](#så-ska-backend-byggas)
- [Säkerhet](#säkerhet)
- [Testning](#testning)
- [Kommandon](#kommandon)
- [CI](#ci)
- [Konventioner](#konventioner)

## Stack

| Del | Val |
|---|---|
| Ramverk | Spring Boot 4.1.1 |
| Java | 25 |
| Bygg | Maven, jar-packaging |
| Konfiguration | YAML |
| Databas | PostgreSQL 18 |
| Migrationer | Flyway |
| Auth | Spring Security med egenutfärdade JWT |
| Test | JUnit 5, AssertJ, MockMvc, Testcontainers |

Frontend är inte bestämd än.

## Struktur

Monorepo med backend och frontend som separata mappar.

```
matchday/
├── .github/
│   └── workflows/        CI
├── backend/              Spring Boot-applikationen
│   ├── .mvn/
│   ├── src/
│   ├── .gitattributes
│   ├── .gitignore
│   ├── HELP.md
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
├── frontend/             Tom, stack inte vald
├── .gitattributes
├── .gitignore
├── compose.yaml          Postgres för lokal utveckling
└── README.md
```

`compose.yaml` ligger i roten och delas av hela projektet. Backend pekar ut den via `spring.docker.compose.file` i `application.yml`.

Varje delprojekt har sin egen `.gitignore`. Rotfilen tar hand om IDE-skräp och miljöfiler.

## Kom igång

Krav: JDK 25, Docker, Docker Compose.

```bash
git clone git@github-privat:Fredalfsson/matchday.git
cd matchday
docker compose up -d
cd backend
./mvnw spring-boot:run
```

Applikationen lyssnar på `http://localhost:8080`. Health-endpointen ligger på `/actuator/health`.

Flyway kör migrationerna automatiskt vid uppstart.

### Miljövariabler

Kopiera `.env.example` till `.env` och fyll i värdena. `.env` är gitignorerad och ska aldrig committas.

| Variabel | Används till |
|---|---|
| `JWT_SECRET` | Signering av tokens |
| `NVD_API_KEY` | OWASP Dependency-Check |

## Så ska backend byggas

Det här kapitlet är projektets arkitekturkontrakt. All ny kod i `backend/` följer det.

### Paket per feature

Koden delas efter domän, inte efter lagertyp. En ändring i betting berör en mapp, inte tre.

```
se/matchday/backend/
├── bet/
│   ├── BetController.java
│   ├── BetService.java
│   ├── BetRepository.java
│   ├── Bet.java              entity
│   └── dto/
│       ├── PlaceBetRequest.java
│       └── BetResponse.java
├── match/
├── message/
└── user/
```

Alltså inga mappar som heter `controllers/`, `services/` eller `entities/`.

Klasser deklareras package-private där det går, så att kompilatorn upprätthåller featuregränserna. Bara det som verkligen ska nås utifrån är `public`.

### Tre lager

```
HTTP  →  Controller  →  Service  →  Repository  →  DB
         [DTO]          [DTO→Entity]  [Entity]
```

| Lager | Ansvar | Känner inte till |
|---|---|---|
| Controller | HTTP, statuskoder, formvalidering | Databasen, affärsregler |
| Service | Affärsregler, transaktioner, mappning | HTTP, statuskoder |
| Repository | Databasfrågor | Affärsregler, HTTP |

Testet på att gränsen är ren: går det att byta ut REST mot ett CLI utan att röra servicen?

### Datagränsen

**Entiteter lämnar aldrig servicelagret.** En `Bet` serialiseras inte till JSON och en `PlaceBetRequest` sparas inte till databasen.

Skälet är säkerhetsmässigt. Utan DTO-gräns kan en klient skicka `{"status": "WON"}` och binda direkt mot entiteten, och nya kolumner läcker ut i API-svar utan att någon märker det.

DTO:er skrivs som `record`. Inkommande DTO validerar med Bean Validation. Utgående DTO har en statisk `from`-metod som mappar från entiteten. Mappningen skrivs för hand, ingen MapStruct eller ModelMapper.

**Användar-ID kommer alltid från token**, aldrig från request-bodyn.

### Transaktioner och fel

`@Transactional` sitter på servicen, inte på controllern och inte på repositoryt. Servicen definierar vad som utgör en komplett affärshändelse. Läsoperationer märks `@Transactional(readOnly = true)`.

Servicen kastar domänundantag som `BetNotFoundException` och `BettingClosedException`. En `@RestControllerAdvice` översätter dem till HTTP-status med `ProblemDetail` enligt RFC 9457. Controllern innehåller inga `if`-satser om affärsregler.

Felmeddelanden till klienten är generiska. Inga stacktraces, inga SQL-fel, inga ID:n som avslöjar att ett objekt finns.

### Controller

Returnerar `ResponseEntity`, konsekvent i alla metoder. `POST` svarar `201 Created` med `Location`-header.

### Pengar och samtidighet

`BigDecimal` för alla belopp, aldrig `double`. Kolumntypen är `NUMERIC(19,4)` med `CHECK (amount > 0)` i databasen utöver `@DecimalMin` i DTO:n. Validering i två lager.

Entiteter som kan ändras samtidigt har `@Version` för optimistisk låsning.

Skrivande endpoints accepterar en `Idempotency-Key`-header med unique constraint i databasen, så att ett omskickat anrop inte skapar dubbla bets.

Listande endpoints tar `Pageable` med tak på sidstorleken.

## Säkerhet

- Stateless sessions, ingen serverside-session
- CSRF avstängt eftersom API:t är rent stateless
- CORS explicit konfigurerad, aldrig `*`
- Roller mappas från JWT-claim via `JwtAuthenticationConverter`
- Metodnivåsäkerhet med `@PreAuthorize`, inte bara filterkedjan
- Ägarskapskontroll på objektnivå, med 404 istället för 403 så att objektets existens inte avslöjas

Ägarskapskontrollen adresserar Broken Access Control, nummer ett på OWASP Top 10. Rollen säger att någon får hämta bets, inte att just den användaren får hämta just det betet.

## Testning

Projektet drivs test-first.

| Nivå | Verktyg | Vad testet bevisar |
|---|---|---|
| Controller | MockMvc, `spring-boot-starter-security-test` | Statuskoder, JSON-form, validering, rollstyrning |
| Service | JUnit och Mockito | Affärsregler, utan Spring-kontext |
| Repository | `@DataJpaTest` med Testcontainers | Frågor och migrationer mot riktig Postgres |

Testerna körs mot samma Postgres-version som produktionen via Testcontainers. H2 används inte, eftersom dialekter beter sig olika kring låsning och transaktioner.

Rollmatrisen är exekverbar dokumentation. Varje skyddad endpoint har minst ett test som bevisar att fel roll ger 403.

Coverage-gaten ligger i `pom.xml` och failar bygget under gränsen. Den står på 0 tills de första testerna finns och höjs sedan stegvis mot 70 procent.

## Kommandon

Kör från `backend/`.

| Kommando | Vad som händer |
|---|---|
| `./mvnw test` | Enhetstester |
| `./mvnw verify` | Formatering, tester, coverage-rapport och gate |
| `./mvnw spring-boot:run` | Startar appen och Postgres via Compose |
| `./mvnw spotless:apply` | Fixar formatering |
| `./mvnw dependency-check:check` | CVE-scanning, tar 10 till 20 minuter första gången |

Coverage-rapporten hamnar i `target/site/jacoco/index.html`.

## CI

`.github/workflows/ci.yml` kör `./mvnw verify` på varje push till `main` och på varje pull request.

Planerat, läggs till stegvis:

- Branch protection på `main` som kräver grön CI
- Dependabot för Maven-beroenden och GitHub Actions
- CodeQL för statisk kodanalys
- OWASP Dependency-Check som nattligt jobb

Dependency-check körs schemalagt istället för på varje push, eftersom nya CVE:er publiceras utan att koden ändras.

## Konventioner

**Migrationer.** Flyway äger schemat. Hibernate står på `ddl-auto: validate` och får bara kontrollera att entiteterna stämmer. Nya migrationer numreras `V2__`, `V3__` och redigeras aldrig i efterhand.

**Formatering.** Spotless med google-java-format körs i `validate`-fasen. Kör `./mvnw spotless:apply` innan commit.

**Commits.** Conventional Commits: `feat:`, `fix:`, `test:`, `chore:`, `docs:`.

**Branches.** En branch per uppgift, PR mot `main`, minst en granskare.