# WaRP-Entwicklungsumgebung mit Docker und Hot Reload

Diese Dokumentation beschreibt die vollständige lokale Docker-Entwicklungsumgebung aus `docker-compose.dev.yaml`. Sie ist von den Produktions-Dockerfiles und der produktionsnahen `docker-compose.yaml` getrennt.

## Enthaltene Komponenten

Die Entwicklungsumgebung startet:

- fünf Spring-Boot-Services
- React-/TypeScript-Frontend mit Vite und HMR
- vier getrennte PostgreSQL-18-Instanzen
- Mailpit als lokalen SMTP-Testserver
- Gradle Continuous Build und Spring Boot DevTools

## Voraussetzungen

- Docker mit Docker Compose V2
- freie Ports `5173`, `8025`, `1025`, `8080` bis `8084` und `5434` bis `5437`

Java, Gradle, Node.js und npm müssen für diesen Modus nicht zwingend auf dem Host installiert sein.

## Erstkonfiguration

Im Projektstamm:

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env
```

Die Projekt-`.env` enthält getrennte lokale Datenbanknamen, Benutzer und Passwörter für die vier persistierenden Services.

Für die Karte kann in `frontend/.env` ein öffentlicher MapTiler-Key gesetzt werden:

```dotenv
VITE_MAPTILER_PUBLIC_API_KEY=your_maptiler_api_key
```

Variablen mit `VITE_` landen im Browser-Bundle und dürfen keine Secrets enthalten.

## Start

```bash
docker compose -f docker-compose.dev.yaml up --build
```

Im Hintergrund starten:

```bash
docker compose -f docker-compose.dev.yaml up --build -d
```

## Lokale Adressen

| Komponente | Adresse |
|---|---|
| Frontend | `http://localhost:5173` |
| BFF | `http://localhost:8080` |
| Identity-Service | `http://localhost:8081` |
| Routing-Service | `http://localhost:8082` |
| Weather-Service | `http://localhost:8083` |
| Storage-Service | `http://localhost:8084` |
| Mailpit-Weboberfläche | `http://localhost:8025` |
| Mailpit SMTP | `localhost:1025` |

## Hot-Reload-Verhalten

### Frontend

Änderungen unter `frontend/` werden von Vite erkannt und per HMR in den Browser übernommen.

Der Container startet Vite mit:

```text
frontend/vite.config.dev.ts
```

Der Entwicklungsproxy leitet weiter:

```text
/api     -> http://bff-service:8080
/graphql -> http://bff-service:8080
```

`frontend/docker-entrypoint.dev.sh` vergleicht `package-lock.json` mit dem Stand im `node_modules`-Volume und führt bei Bedarf automatisch `npm ci` aus.

### Backend

Für jeden Spring-Service laufen im Entwicklungscontainer:

1. ein initialer Gradle-Compile-Schritt,
2. `bootRun` mit Spring Boot DevTools,
3. ein Gradle Continuous Build für die Klassen des jeweiligen Services.

Änderungen an Java- oder Resource-Dateien werden kompiliert. Spring Boot DevTools startet danach den betroffenen Application Context neu.

DevTools wird ausschließlich über `infrastructure/docker/dev/spring-dev.init.gradle` eingebunden. Es wird nicht in reguläre `build.gradle`-Dateien oder produktive JARs aufgenommen.

## Wann ist ein Neustart nötig?

Ein Container-Neustart oder Rebuild ist insbesondere nötig bei Änderungen an:

- `build.gradle`
- `settings.gradle`
- `gradle.properties`
- dem Gradle Wrapper
- Dockerfiles oder Entrypoint-Skripten
- `docker-compose.dev.yaml`
- Frontend-Abhängigkeiten, falls die automatische Synchronisierung nicht greift

Nur einen Service neu erstellen:

```bash
docker compose -f docker-compose.dev.yaml up --build --force-recreate weather-service
```

## Nützliche Befehle

Status anzeigen:

```bash
docker compose -f docker-compose.dev.yaml ps
```

Logs aller Container verfolgen:

```bash
docker compose -f docker-compose.dev.yaml logs -f
```

Logs eines Services verfolgen:

```bash
docker compose -f docker-compose.dev.yaml logs -f weather-service
```

Nur einen Dienst und seine Abhängigkeiten starten:

```bash
docker compose -f docker-compose.dev.yaml up --build weather-service
```

Frontend neu bauen:

```bash
docker compose -f docker-compose.dev.yaml up --build --force-recreate frontend
```

Beenden:

```bash
docker compose -f docker-compose.dev.yaml down
```

## Datenbanken

| Service | Compose-Service | Datenbank | Host-Port |
|---|---|---|---:|
| Identity | `identity-db` | `warp_identity` | `5434` |
| Routing | `routing-db` | `warp_routing` | `5435` |
| Weather | `weather-db` | `warp_weather` | `5436` |
| Storage | `storage-db` | `warp_storage` | `5437` |

Innerhalb des Compose-Netzes verwenden die Anwendungen den jeweiligen Compose-Servicenamen und Port `5432`, nicht `localhost`.

Beispiel vom Host:

```text
jdbc:postgresql://localhost:5434/warp_identity
```

Datenbank-Logs anzeigen:

```bash
docker compose -f docker-compose.dev.yaml logs -f identity-db
```

Alle lokalen Daten und Entwicklungs-Volumes löschen:

```bash
docker compose -f docker-compose.dev.yaml down -v
```

> **Achtung:** Dies löscht Datenbanken, Mailpit-Daten, Gradle-Caches, Build-Ausgaben und das Frontend-`node_modules`-Volume.

## Flyway

Die Migrationen liegen servicebezogen unter:

```text
services/<service>/src/main/resources/db/migration
```

Beim Start führt Flyway fehlende Migrationen aus. Anschließend validiert Hibernate das Schema. Bereits in gemeinsam verwendeten Umgebungen ausgeführte Migrationen dürfen nicht verändert werden.

## Mailpit

Der Identity-Service ist in der Entwicklungsumgebung vorkonfiguriert mit:

```text
MAIL_HOST=mailpit
MAIL_PORT=1025
MAIL_SMTP_AUTH=false
MAIL_SMTP_STARTTLS_ENABLED=false
MAIL_FROM=no-reply@warp.local
```

E-Mails werden lokal von Mailpit entgegengenommen und unter `http://localhost:8025` angezeigt. Es erfolgt kein Versand ins Internet.

Mailpit-Logs:

```bash
docker compose -f docker-compose.dev.yaml logs -f mailpit
```

## Healthchecks prüfen

```bash
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:8081/actuator/health/readiness
curl http://localhost:8082/actuator/health/readiness
curl http://localhost:8083/actuator/health/readiness
curl http://localhost:8084/actuator/health/readiness
```

BFF-Gesamtstatus:

```bash
curl http://localhost:8080/api/v1/system/health
```

GraphQL-Systemstatus:

```bash
curl \
  -X POST \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/graphql-response+json' \
  --data '{"query":"query SystemHealth { systemHealth { status healthyServices totalServices } }","operationName":"SystemHealth","variables":{}}' \
  http://localhost:8080/graphql
```

## Frontend-Prüfung außerhalb des Containers

Wenn Node.js 22.22 oder neuer lokal installiert ist:

```bash
cd frontend
npm ci
npm run check
```

`npm run check` führt die TypeScript-Prüfung und den Produktions-Build aus.

## Typische Probleme

### Port bereits belegt

Auf macOS oder Linux:

```bash
lsof -iTCP:8080 -sTCP:LISTEN
```

Dann den Prozess beenden oder die kollidierende Anwendung stoppen.

### Frontend erreicht den BFF nicht

Prüfen:

```bash
docker compose -f docker-compose.dev.yaml ps
docker compose -f docker-compose.dev.yaml logs -f frontend bff-service
curl http://localhost:8080/actuator/health/readiness
```

Die Docker-Vite-Konfiguration muss `/api` und `/graphql` an `http://bff-service:8080` weiterleiten.

### Änderungen werden nicht erkannt

Das Frontend verwendet im Docker-Modus Dateisystem-Polling über:

```text
VITE_USE_POLLING=true
```

Bei Backend-Problemen die Logs des betroffenen Services prüfen und ihn bei Änderungen an Builddateien neu erstellen.

### Abhängigkeiten sind veraltet

Frontend neu erstellen:

```bash
docker compose -f docker-compose.dev.yaml up --build --force-recreate frontend
```

Backend-Service neu erstellen:

```bash
docker compose -f docker-compose.dev.yaml up --build --force-recreate identity-service
```

### Migration schlägt fehl

```bash
docker compose -f docker-compose.dev.yaml logs -f identity-service identity-db
```

Prüfen:

- Stimmen Variablen in `.env`?
- Ist die Datenbank erreichbar und gesund?
- Wurde eine bereits ausgeführte Migration verändert?
- Passt das Flyway-Schema zu den JPA-Entitäten?

## Trennung von Entwicklung und Produktion

Die Entwicklungsumgebung verwendet:

```text
docker-compose.dev.yaml
infrastructure/docker/dev/Dockerfile.spring
infrastructure/docker/dev/spring-dev-entrypoint.sh
infrastructure/docker/dev/spring-dev.init.gradle
frontend/Dockerfile.dev
frontend/docker-entrypoint.dev.sh
frontend/vite.config.dev.ts
```

Die Produktions- beziehungsweise Deployment-Builds verwenden:

```text
docker-compose.yaml
services/*/Dockerfile
infrastructure/docker/production/spring-entrypoint.sh
frontend/Dockerfile
frontend/server.mjs
```

Entwicklungshelfer wie DevTools, Continuous Build und Vite HMR sind nicht Bestandteil der produktiven Images.
