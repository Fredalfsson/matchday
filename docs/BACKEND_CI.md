# CI och säkerhetsverifiering för backend

Matchday håller backendens kvalitetsverifiering åtskild från säkerhetsgranskningen av beroenden.
Det gör att vanliga lokala byggen förblir reproducerbara utan åtkomst till en extern
sårbarhetsdatabas eller en lagrad hemlighet.

## Backend CI

Arbetsflödet `Backend CI` körs för pull requests mot `main`, pushar till `main` och manuella
körningar. Det kör:

```bash
cd backend
./mvnw --batch-mode --no-transfer-progress verify
```

Mavens `verify`-livscykel omfattar kompilering, automatiserade tester, Spotless-kontroll,
generering av JaCoCo-rapport, kvalitetsgrinden på 70 procents radtäckning och paketering av
applikationen. Arbetsflödet laddar upp test- och täckningsrapporter även när verifieringen
misslyckas.

Arbetsflödet har endast läsbehörighet till repot, sparar inte Git-inloggningsuppgifter och ändrar
eller committar aldrig källkod.

## Säkerhetsgranskning av backendens beroenden

Arbetsflödet `Backend Security` körs för pull requests mot `main`, pushar till `main`, varje måndag
och vid manuell start. Den kör OWASP Dependency-Check uttryckligen och misslyckas om ett beroende
med CVSS-värde 7 eller högre upptäcks. Genom att köra kontrollen för varje pull request rapporterar
en framtida obligatorisk branch protection-kontroll alltid ett resultat i stället för att förbli
väntande.

Arbetsflödet kräver följande repohemlighet (repository secret) i GitHub Actions:

```text
NVD_API_KEY
```

Begär en kostnadsfri nyckel via
[National Vulnerability Database](https://nvd.nist.gov/developers/request-an-api-key) och lägg till
den under repots **Settings > Secrets and variables > Actions**. Maven-pluginen läser nyckeln från
miljön och den får aldrig committas till repot.

Dependency-Check-data cachelagras mellan workflowkörningar eftersom den första hämtningen från
NVD kan vara stor. Om `NVD_API_KEY` saknas misslyckas arbetsflödet uttryckligen i stället för att
säkerhetskontrollen hoppas över utan varning.

Det nuvarande teamflödet utgår från branches i samma repository. GitHub skickar inte vanliga
Actions repository secrets till workflows som startas från forks eller av Dependabot. Innan
externa fork-baserade bidrag tillåts eller Dependabot aktiveras behöver teamet därför besluta om en
separat secret- eller workflowstrategi. Använd inte `pull_request_target` för att exponera en secret
för kod från en obetrodd pull request.

## Skydd för pull requests

När båda arbetsflödena har körts framgångsrikt minst en gång konfigureras branch protection för
`main` med följande obligatoriska kontroller före merge:

- `Backend verification`
- `Backend dependency audit`

Kräv pull request och minst en godkännande review enligt teamets överenskomna arbetssätt. Det
exakta skyddet konfigureras i GitHub och ska hållas i linje med detta dokument.

## Leveranskedjekontroller för GitHub Actions

Varje GitHub Action är pinnad till ett fullständigt commit-SHA och motsvarande releaseversion
anges i en kommentar. En uppdatering kräver därför att releasen granskas och att både SHA-värdet
och versionskommentaren uppdateras.

Maven Wrapper pinnar Maven-versionen och verifierar den nedladdade distributionen mot en SHA-256-
checksumma. När Maven-versionen uppdateras måste även checksumman hämtas från en verifierad
Apache-release och uppdateras i `backend/.mvn/wrapper/maven-wrapper.properties`.
