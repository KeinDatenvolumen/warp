# WaRP: Weather-aware Route Planning

WaRP ist eine mobile Webanwendung zur wetterabhängigen Planung von Routen
für Fahrrad, Motorrad und Wandern.

Eine Route wird mit zeitlich und räumlich passenden Wetterprognosen
verknüpft. Nutzer können einen Wetterscore erhalten, alternative
Abfahrtszeiten vergleichen und berechnete Routen speichern.

## Projektstatus

Das Projekt befindet sich aktuell im Aufbau des technischen Fundaments.

Bereits vorhanden sind:

- Gradle-Multi-Project-Build
- fünf getrennte Spring-Boot-Services
- React-Frontend mit Vite
- Health-, Readiness- und Liveness-Endpunkte
- Live-Dashboard für die Servicezustände
- Dockerfiles für alle Services und das Frontend
- GitHub-Actions-Workflows
- getrennte Heroku-Deployments

Fachliche Funktionen wie Registrierung, Routing, Wetterauswertung und
gespeicherte Routen werden schrittweise ergänzt.

## Architektur

Das Backend besteht aus fünf unabhängigen Spring-Boot-Anwendungen:

- `bff-service`
- `identity-service`
- `routing-service`
- `weather-service`
- `storage-service`

Das React-Frontend kommuniziert ausschließlich mit dem BFF.

Der lokale Kommunikationsfluss lautet:

```text
Browser
  -> React-Frontend
  -> BFF
  -> Identity-, Routing-, Weather- und Storage-Service
```

## Verwendete Versionen

Für die lokale Entwicklung werden benötigt:

- Java 21 LTS
- Gradle 9.8.0 über den mitgelieferten Gradle Wrapper
- Node.js 22.12.0 oder neuer
- npm
- Git
- Docker und Docker Compose für Containerprüfungen
- curl für die automatischen lokalen Healthchecks

Eine globale Gradle-Installation ist nicht erforderlich. Verwendet wird
immer der Gradle Wrapper aus dem Repository.

## Lokale Ports

- Frontend: `5173`
- BFF-Service: `8080`
- Identity-Service: `8081`
- Routing-Service: `8082`
- Weather-Service: `8083`
- Storage-Service: `8084`

Die Ports müssen beim lokalen Start verfügbar sein.

## Projektstruktur

```text
warp/
├── .github/
│   └── workflows/
├── contracts/
│   ├── examples/
│   ├── graphql/
│   └── openapi/
├── frontend/
├── infrastructure/
│   ├── docker/
│   └── heroku/
├── scripts/
├── services/
│   ├── bff-service/
│   ├── identity-service/
│   ├── routing-service/
│   ├── weather-service/
│   └── storage-service/
├── build.gradle
├── settings.gradle
├── gradlew
└── gradlew.bat
```

## Erste Schritte nach dem Klonen

### 1. Repository klonen

```bash
git clone URL-DES-REPOSITORIES
cd warp
```

Ersetze `URL-DES-REPOSITORIES` durch die tatsächliche HTTPS- oder
SSH-Adresse des GitHub-Repositorys.

### 2. Voraussetzungen prüfen

Unter Linux, macOS, WSL oder Git Bash:

```bash
java -version
node --version
npm --version
git --version
docker --version
docker compose version
```

Unter Windows PowerShell können dieselben Befehle verwendet werden.

Erwartet werden mindestens:

```text
Java 21
Node.js 22.12.0
Docker mit Docker Compose V2
```

### 3. Gradle Wrapper prüfen

Unter Linux, macOS, WSL oder Git Bash:

```bash
chmod +x gradlew
./gradlew --version
```

Unter Windows PowerShell:

```powershell
.\gradlew.bat --version
```

Die Ausgabe muss Gradle 9.8.0 und eine Java-21-Laufzeit anzeigen.

### 4. Frontend-Abhängigkeiten installieren

```bash
cd frontend
npm ci
cd ..
```

`npm ci` verwendet die eingecheckte `package-lock.json` und installiert
die bekannten Abhängigkeitsversionen reproduzierbar.

Die lokalen Startskripte führen `npm ci` automatisch aus, wenn
`frontend/node_modules` noch nicht existiert. Die manuelle Ausführung
ist trotzdem sinnvoll, um Installationsprobleme früh zu erkennen.

### 5. Gesamtes Projekt bauen

Unter Linux, macOS, WSL oder Git Bash:

```bash
./gradlew clean build
```

Unter Windows PowerShell:

```powershell
.\gradlew.bat clean build
```

Frontend prüfen:

```bash
cd frontend
npm run build
cd ..
```

Erst wenn Backend und Frontend erfolgreich gebaut werden, sollte mit
der eigentlichen Entwicklung begonnen werden.

## Alle Services und das Frontend starten

### Linux, macOS, WSL oder Git Bash

```bash
./scripts/start-local.sh
```

Falls das Skript noch nicht ausführbar ist:

```bash
chmod +x scripts/start-local.sh
./scripts/start-local.sh
```

### Windows PowerShell

```powershell
.\scripts\start-local.ps1
```

Falls die Ausführung lokaler Skripte blockiert wird:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

Das Startskript:

1. prüft Java, Node.js, npm und den Gradle Wrapper,
2. prüft die benötigten Ports,
3. installiert bei Bedarf die Frontend-Abhängigkeiten,
4. startet alle fünf Spring-Boot-Services,
5. startet das React-Frontend,
6. wartet auf die Readiness-Endpunkte,
7. zeigt die lokalen URLs an.

Das Skript bleibt geöffnet. Mit `Strg+C` werden alle Prozesse beendet,
die durch das Skript gestartet wurden.

Die lokalen Logs befinden sich unter:

```text
logs/local/
```

## Anwendung öffnen

Nach einem erfolgreichen Start ist das Frontend erreichbar unter:

```text
http://localhost:5173
```

Der zusammengefasste Servicezustand ist erreichbar unter:

```text
http://localhost:8080/api/v1/system/health
```

Das Frontend aktualisiert die Servicezustände automatisch.

## Healthchecks

### BFF-Service

```text
http://localhost:8080/actuator/health/readiness
```

### Identity-Service

```text
http://localhost:8081/actuator/health/readiness
```

### Routing-Service

```text
http://localhost:8082/actuator/health/readiness
```

### Weather-Service

```text
http://localhost:8083/actuator/health/readiness
```

### Storage-Service

```text
http://localhost:8084/actuator/health/readiness
```

Ein erfolgreicher Readinesscheck liefert:

```json
{
  "status": "UP"
}
```

## Services einzeln starten

Für die normale Entwicklung ist das gemeinsame Startskript vorgesehen.
Ein einzelner Service kann bei Bedarf aber auch separat gestartet werden.

### BFF-Service

```bash
./gradlew :services:bff-service:bootRun
```

### Identity-Service

```bash
./gradlew :services:identity-service:bootRun
```

### Routing-Service

```bash
./gradlew :services:routing-service:bootRun
```

### Weather-Service

```bash
./gradlew :services:weather-service:bootRun
```

### Storage-Service

```bash
./gradlew :services:storage-service:bootRun
```

Unter Windows wird jeweils `.\gradlew.bat` statt `./gradlew` verwendet.

### Frontend

```bash
cd frontend
npm run dev
```

Das Frontend erwartet den BFF lokal unter:

```text
http://localhost:8080
```

Vite leitet lokale Requests mit dem Pfad `/api` automatisch an den BFF
weiter.

## Tests und Builds

### Alle Backend-Tests

```bash
./gradlew clean test
```

### Vollständiger Backend-Build

```bash
./gradlew clean build
```

### Nur einen Service testen

Beispiel für den Weather-Service:

```bash
./gradlew :services:weather-service:test
```

### Nur einen Service bauen

```bash
./gradlew :services:weather-service:build
```

### Frontend bauen

```bash
cd frontend
npm ci
npm run build
```

Automatisierte Tests dürfen keine echten externen APIs, kostenpflichtigen
Dienste oder echten E-Mail-Versand verwenden.

## Umgang mit Änderungen

### Vor Beginn einer Aufgabe

Wechsle auf den aktuellen Zielbranch und aktualisiere ihn:

```bash
git switch system/live
git pull
```

Erstelle anschließend einen kurzen Feature-Branch:

```bash
git switch -c feature/WARP-123-kurze-beschreibung
```

Beispiele:

```text
feature/WARP-008-registration
feature/WARP-015-route-calculation
fix/WARP-027-csrf-validation
```

Nicht direkt auf `system/live` entwickeln oder pushen.

### Während der Entwicklung

Änderungen sollten:

- klein und nachvollziehbar bleiben,
- nur den betroffenen Service verändern,
- bestehende Funktionen nicht unnötig ersetzen,
- passende Tests enthalten,
- API-Verträge aktualisieren, wenn sich Schnittstellen ändern,
- keine Zugangsdaten oder Tokens enthalten.

Prüfe regelmäßig:

```bash
git status
git diff
```

### Vor dem Commit

Backend prüfen:

```bash
./gradlew clean build
```

Frontend prüfen, wenn das Frontend betroffen ist:

```bash
cd frontend
npm ci
npm run build
cd ..
```

Zusätzlich sollte geprüft werden:

```bash
git diff --check
```

### Commit erstellen

Empfohlene Präfixe:

- `feat:` für neue Funktionen
- `fix:` für Fehlerbehebungen
- `test:` für Tests
- `docs:` für Dokumentation
- `chore:` für technische Wartung
- `refactor:` für interne Umbauten ohne neue Funktion

Beispiel:

```bash
git add .
git commit -m "feat: add weather evaluation endpoint"
```

### Feature-Branch pushen

```bash
git push -u origin HEAD
```

Danach wird auf GitHub ein Pull Request mit folgendem Ziel erstellt:

```text
system/live
```

## Pull Requests und Deployment

Ein Pull Request nach `system/live` führt automatisierte Prüfungen aus:

- Backend-Tests
- Backend-Build
- Frontend-Build
- Docker-Builds

Das Heroku-Deployment erfolgt nicht direkt aus dem ungeprüften
Pull-Request-Code.

Nach dem Merge entsteht ein Push auf `system/live`. Abhängig von den
geänderten Pfaden werden die betroffenen Services und das Frontend über
GitHub Actions zu Heroku deployt.

Eine reine Änderung an einem Service soll nicht alle anderen Services
unnötig deployen.

## Konflikte vor dem Merge vermeiden

Wenn sich `system/live` während der Bearbeitung geändert hat:

```bash
git fetch origin
git rebase origin/system/live
```

Konflikte werden lokal gelöst. Danach:

```bash
git add DATEI
git rebase --continue
```

Wenn der Branch bereits gepusht wurde:

```bash
git push --force-with-lease
```

`--force-with-lease` darf nur auf dem eigenen Feature-Branch verwendet
werden, niemals auf `system/live`.

## Abhängigkeiten ändern

### Java-Abhängigkeiten

Abhängigkeiten werden in der `build.gradle` des betroffenen Services
ergänzt.

Danach:

```bash
./gradlew clean build
```

Neue Abhängigkeiten sollen nur eingeführt werden, wenn sie für die
Aufgabe tatsächlich benötigt werden.

### Frontend-Abhängigkeiten

Neue Frontend-Abhängigkeiten werden im Verzeichnis `frontend` installiert:

```bash
cd frontend
npm install PAKETNAME
```

Dabei müssen sowohl `package.json` als auch `package-lock.json`
committed werden.

`node_modules` wird niemals committed.

## API-Verträge

Neue Funktionen werden zuerst vertraglich beschrieben.

Die Verträge liegen unter:

```text
contracts/
├── examples/
├── graphql/
└── openapi/
```

- GraphQL-Verträge gehören nach `contracts/graphql`.
- REST-Verträge gehören nach `contracts/openapi`.
- Beispielantworten externer APIs gehören nach `contracts/examples`.

Implementierung und Vertrag müssen konsistent bleiben.

## Umgebungsvariablen und Secrets

Echte Secrets dürfen niemals committed werden.

Dazu gehören insbesondere:

- Passwörter
- API-Schlüssel
- Heroku-Tokens
- Session-Werte
- private Schlüssel
- echte `.env`-Dateien
- produktive Datenbankzugangsdaten

Lokale Beispielwerte dürfen ausschließlich in einer `.env.example`
dokumentiert werden.

## Docker

Jeder Backend-Service und das Frontend besitzen ein eigenes Dockerfile.

Ein einzelnes Image wird immer aus dem Projektstamm gebaut.

Beispiel:

```bash
docker build \
  -f services/weather-service/Dockerfile \
  -t warp-weather:local \
  .
```

Frontend:

```bash
docker build \
  -f frontend/Dockerfile \
  --build-arg VITE_BFF_BASE_URL=http://localhost:8080 \
  -t warp-frontend:local \
  .
```

## Projektdump erstellen

Für die Übergabe an einen neuen Entwicklungs-Chat kann ein Projektdump
erstellt werden:

```bash
./scripts/create-project-dump.sh
```

Das Ergebnis wird als folgende Datei im Projektstamm gespeichert:

```text
dump.txt
```

Der Dump schließt Buildverzeichnisse, Abhängigkeiten, Binärdateien und
bekannte sensible Dateien aus.

Trotzdem muss `dump.txt` vor dem Weitergeben manuell auf Zugangsdaten,
Tokens und personenbezogene Informationen geprüft werden.

## Typische Probleme

### Java-Version ist falsch

Prüfen:

```bash
java -version
./gradlew --version
```

Beide Ausgaben müssen auf Java 21 verweisen.

Falls `JAVA_HOME` falsch gesetzt ist, muss es auf das installierte JDK 21
zeigen.

### Gradle Wrapper fehlt

Diese Dateien müssen im Repository vorhanden sein:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

Insbesondere `gradle-wrapper.jar` muss committed sein.

### Port ist bereits belegt

Benötigte Ports:

```text
5173
8080
8081
8082
8083
8084
```

Unter Linux oder macOS kann ein belegter Port so untersucht werden:

```bash
lsof -iTCP:8080 -sTCP:LISTEN
```

Unter Windows PowerShell:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
```

### Frontend zeigt den BFF als nicht erreichbar

Prüfen:

```bash
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:8080/api/v1/system/health
```

Falls der BFF läuft, prüfe die Browserkonsole und den Vite-Proxy.

### Ein Service bleibt beim Start auf DOWN

Prüfe die zugehörige Datei unter:

```text
logs/local/
```

Danach kann der Service einzeln gestartet werden, damit seine Ausgabe
direkt im Terminal sichtbar ist.

## Sicherheitsregeln

- Keine Secrets im Repository
- Keine Passwörter oder Tokens in Logs
- Keine Datenbank eines anderen Services direkt verwenden
- Keine internen Services direkt aus dem Browser aufrufen
- Externe API-Schlüssel nur serverseitig verwenden
- Eingaben validieren
- REST-Fehler in stabile eigene Fehlercodes übersetzen
- Keine Stacktraces an Clients ausgeben
- Geschützte Operationen autorisieren
- Besitzprüfungen bei benutzerbezogenen Daten durchführen