# Matchday

Applikation för att se kommande matcher i allsvenskan, samt skapa eller gå med i grupper för att chatta med likasinnade.

## Stack
### Backend Stack

Spring Boot 4.1.1, Java 25, Maven, PostgreSQL 18, Flyway, Spring Security med JWT.

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

### Frontend
..
