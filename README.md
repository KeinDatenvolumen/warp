# WaRP: Weather-aware Route Planning

WaRP ist eine mobile Webanwendung zur wetterabhängigen Planung von Fahrrad-, Motorrad- und Wanderrouten. Geplante Routen sollen mit räumlich und zeitlich passenden Wetterdaten verknüpft, bewertet und gespeichert werden.

> **Projektstatus:** Das technische Fundament steht. Fachliche Funktionen wie produktive Registrierung und Anmeldung, Routenberechnung, Wetterbewertung und gespeicherte Routen werden schrittweise ergänzt.

## Aktueller Funktionsumfang

Bereits vorhanden sind:

- Gradle-Multi-Project-Build mit Java 21
- fünf getrennte Spring-Boot-Services
- React-Frontend mit TypeScript im Strict Mode und Vite
- REST- und GraphQL-Anbindung des Frontends an den BFF
- Health-, Readiness- und Liveness-Endpunkte
- REST- und GraphQL-Dashboard für den Systemstatus
- Leaflet-Karte mit MapTiler-Anbindung
- vorläufige lokale Testanmeldung und geschützte Frontend-Routen
- PostgreSQL-Datenbank pro persistierendem Service
- Flyway-Migrationen und Hibernate-Schemavalidierung
- lokale Docker-Entwicklungsumgebung mit Hot Reload
- Mailpit als lokaler SMTP- und E-Mail-Testserver
- Produktions-Dockerfiles und getrennte Heroku-Deployments

Noch nicht produktiv umgesetzt sind insbesondere:

- echte Registrierung, Anmeldung und Session-Verwaltung
- fachliche Routenberechnung
- Wetterauswertung und Wetterscore
- persistente Routenverwaltung im Frontend

## Architektur

```text
Browser
  -> React-/TypeScript-Frontend
  -> BFF-Service
  -> Identity-, Routing-, Weather- und Storage-Service
```

Das Frontend kommuniziert ausschließlich mit dem BFF. Interne Services werden nicht direkt aus dem Browser angesprochen.

### Services

| Service | Aufgabe | Port | Datenbank |
|---|---|---:|---|
| `bff-service` | öffentlicher Einstiegspunkt, REST und GraphQL | `8080` | keine |
| `identity-service` | Benutzer, Anmeldung und Identität | `8081` | `warp_identity` |
| `routing-service` | Routenberechnung | `8082` | `warp_routing` |
| `weather-service` | Wetterdaten und Auswertung | `8083` | `warp_weather` |
| `storage-service` | gespeicherte Routen und persistente Fachdaten | `8084` | `warp_storage` |
| Frontend (SPA) | Weboberfläche (React & Vite) | `5173` | keine |
| MPA | Multi-Page Application (Express, EJS & GraphQL) | `8085` | keine |
| Mailpit UI | lokale E-Mail-Ansicht | `8025` | lokales Volume |
| Mailpit SMTP | lokaler SMTP-Eingang | `1025` | lokales Volume |
| RabbitMQ | Message Queue für den E-Mail-Versand (UI auf `15672`) | `5672` | lokales Volume |

Die PostgreSQL-Instanzen sind in der Docker-Entwicklung zusätzlich über die Host-Ports `5434` bis `5437` erreichbar.

## Technologieübersicht

### Backend

- Java 21
- Spring Boot 4.1.1
- Gradle 9.8.0 über den Gradle Wrapper
- Spring GraphQL im BFF
- PostgreSQL 18 in der Docker-Entwicklung
- Flyway für Datenbankmigrationen
- Hibernate ausschließlich zur Schemavalidierung

### Frontend

- React 19.3
- TypeScript 7 mit Strict Mode
- Vite 8
- React Router 8
- Apollo Client 4
- Leaflet und React Leaflet
- CSS Modules und globale CSS-Variablen

Für die lokale Frontend-Entwicklung wird wegen React Router **Node.js 22.22 oder neuer** empfohlen.

## Projektstruktur

```text
warp/
├── .github/workflows/              # Prüf- und Deployment-Workflows
├── contracts/
│   ├── examples/                   # Beispielantworten externer APIs
│   ├── graphql/                    # öffentlicher GraphQL-Vertrag
│   └── openapi/                    # REST-Verträge
├── frontend/                       # React-/TypeScript-Anwendung (SPA)
├── mpa/                            # Multi-Page Application (Express, EJS, GraphQL)
├── infrastructure/
│   ├── docker/dev/                 # Docker-Hot-Reload für Spring
│   ├── docker/production/          # Produktions-Entrypoints
│   └── heroku/
├── scripts/                        # lokale Starts und Projektdump
├── services/
│   ├── bff-service/
│   ├── identity-service/
│   ├── routing-service/
│   ├── weather-service/
│   └── storage-service/
├── docker-compose.dev.yaml         # vollständige Entwicklungsumgebung
├── docker-compose.yaml             # produktionsnahe Containerumgebung
├── build.gradle
├── settings.gradle
└── gradlew
```

## Voraussetzungen

- Java 21
- Node.js 22.22 oder neuer
- npm
- Docker mit Docker Compose V2
- `curl` für lokale Healthchecks

Eine globale Gradle-Installation ist nicht erforderlich.

## Schnellstart mit Docker

### 1. Lokale Konfiguration anlegen

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env
```

Mindestens der öffentliche MapTiler-Key muss bei Bedarf in `frontend/.env` gesetzt werden:

```dotenv
VITE_MAPTILER_PUBLIC_API_KEY=your_maptiler_api_key
```

Variablen mit `VITE_` werden in das Browser-Bundle übernommen und dürfen keine Secrets enthalten.

### 2. Entwicklungsumgebung starten

```bash
docker compose -f docker-compose.dev.yaml up --build
```

Danach sind erreichbar:

```text
Frontend:    http://localhost:5173
BFF:         http://localhost:8080
Identity:    http://localhost:8081
Routing:     http://localhost:8082
Weather:     http://localhost:8083
Storage:     http://localhost:8084
Mailpit UI:  http://localhost:8025
```

Ausführliche Hinweise zum Entwicklungsmodus stehen in [`README-DEV-WATCH.md`](README-DEV-WATCH.md).

## Lokaler Start ohne Docker

Die vorhandenen Startskripte starten Backend und Frontend direkt auf dem Host:

### macOS, Linux, WSL oder Git Bash

```bash
chmod +x scripts/start-local.sh
./scripts/start-local.sh
```

### Windows PowerShell

```powershell
.\scripts\start-local.ps1
```

Dabei werden Voraussetzungen und Ports geprüft, Services gestartet und Logs unter `logs/local/` abgelegt.

> Die persistenten Services benötigen erreichbare PostgreSQL-Datenbanken. Für den vollständigen Projektbetrieb ist deshalb die Docker-Entwicklungsumgebung der bevorzugte Weg.

## Builds und Prüfungen

### Backend vollständig prüfen

```bash
./gradlew clean build
```

### Backend-Tests

```bash
./gradlew clean test
```

### Einzelnen Service prüfen

```bash
./gradlew :services:weather-service:test
./gradlew :services:weather-service:build
```

### Frontend installieren und vollständig prüfen

```bash
cd frontend
npm ci
npm run check
```

Verfügbare Frontend-Scripts:

```bash
npm run dev        # Vite-Entwicklungsserver
npm run typecheck  # TypeScript-Prüfung ohne Ausgabe
npm run build      # TypeScript-Prüfung und Produktions-Build
npm run check      # typecheck und build
npm run preview    # lokalen Produktions-Build anzeigen
```

Der aktuelle Frontend-Stand enthält noch keine separate automatisierte Test-Suite und keinen Lint-Befehl.

## Frontend-Routen

| Route | Stand |
|---|---|
| `/` | REST-basiertes Service-Dashboard mit automatischer Aktualisierung |
| `/system-status` | GraphQL-basiertes Systemstatus-Dashboard |
| `/route-planner` | Leaflet-Karte; fachliche Routenplanung noch nicht implementiert |
| `/login` | vorläufige lokale Testanmeldung |
| `/profile` | geschützte Platzhalterseite |
| `/saved-routes` | geschützte Platzhalterseite |
| `*` | 404-Seite mit angefragtem Pfad |

Die Testanmeldung speichert ausschließlich zu Entwicklungszwecken den Schlüssel `warp-user-authenticated` in `localStorage`. Sie ist keine produktive Authentifizierung.

## BFF, REST und GraphQL

REST-Systemstatus:

```text
GET /api/v1/system/health
```

GraphQL-Endpunkt:

```text
POST /graphql
```

Der verbindliche GraphQL-Vertrag liegt unter:

```text
contracts/graphql/schema.graphqls
```

Konventionen, Fehlercodes und Transportregeln stehen in `contracts/graphql/README.md`. Neue öffentliche Funktionen werden zuerst im passenden Vertrag beschrieben und anschließend in BFF und Frontend umgesetzt.

## Healthchecks

```text
http://localhost:8080/actuator/health/readiness
http://localhost:8081/actuator/health/readiness
http://localhost:8082/actuator/health/readiness
http://localhost:8083/actuator/health/readiness
http://localhost:8084/actuator/health/readiness
```

Ein erfolgreicher Readinesscheck liefert:

```json
{
  "status": "UP"
}
```

Der Produktionsserver des Frontends stellt zusätzlich bereit:

```text
GET /health
```

## Datenbanken und Migrationen

Jeder persistierende Service besitzt eine eigene PostgreSQL-Datenbank. Ein Service darf nicht direkt auf Tabellen eines anderen Services zugreifen.

Migrationen liegen jeweils unter:

```text
services/<service>/src/main/resources/db/migration
```

Regeln:

1. Schemaänderungen erfolgen ausschließlich über neue Flyway-Migrationen.
2. Bereits ausgeführte Migrationen werden nicht nachträglich verändert.
3. Hibernate bleibt auf `ddl-auto=validate`.
4. Migrationen müssen mit einer leeren und einer vorhandenen Datenbank funktionieren.
5. Produktive Zugangsdaten werden ausschließlich als geschützte Umgebungsvariablen bereitgestellt.

Lokale Daten einschließlich Volumes vollständig löschen:

```bash
docker compose -f docker-compose.dev.yaml down -v
```

> **Achtung:** Dieser Befehl löscht alle lokalen WaRP-Datenbanken und die lokalen Mailpit-Daten unwiderruflich.

## Lokaler Mailserver

Die Docker-Entwicklungsumgebung enthält Mailpit:

```text
SMTP-Host im Compose-Netz: mailpit
SMTP-Port:                1025
Weboberfläche:            http://localhost:8025
```

Der Identity-Service verwendet lokal `no-reply@warp.local` als Absender. Es werden keine E-Mails ins Internet versendet.

## Message Queue (RabbitMQ)

Der Identity-Service stellt E-Mails (Verifikation, Passwort-Reset) nicht mehr
synchron in der Datenbank-Transaktion zu. Er reiht nach dem Commit einen Auftrag
in RabbitMQ ein, ein Konsument stellt ihn per SMTP zu.

```text
Exchange warp.mail -> Queue warp.mail.send -> Konsument (3 Versuche)
                                           -> Dead-Letter-Queue warp.mail.send.dlq
```

- Management-UI lokal: `http://localhost:15672` (Zugang aus `RABBITMQ_USER` und `RABBITMQ_PASSWORD`)
- Broker-Adresse: `RABBITMQ_URL` (`amqp://…`, produktiv `amqps://…`)
- Standard ist die direkte Zustellung nach dem Commit. Die Queue ist abgeschaltet.
- Aktiviert wird sie mit `WARP_MESSAGING_ENABLED=true` und `RABBITMQ_URL`. Lokal setzt das `docker-compose.dev.yaml`.
- Ist die Queue aktiv und der Broker nicht erreichbar, fällt der Service automatisch auf die direkte Zustellung zurück.
- Fehlgeschlagene Aufträge in `warp.mail.send.dlq` sind in der UI sichtbar. Benutzer können die Mail über „erneut senden“ bzw. „Passwort vergessen“ neu anfordern.
- Vertrag: `contracts/asyncapi/mail-events.yaml`

## Umgebungsvariablen und Secrets

- `.env` und `frontend/.env` werden nicht versioniert.
- `.env.example` enthält nur lokale Beispielwerte oder Platzhalter.
- Datenbankzugänge werden pro Service getrennt verwaltet.
- `VITE_*`-Variablen sind öffentlich und dürfen keine geheimen Werte enthalten.
- Tokens, Passwörter, private Schlüssel und produktive Verbindungsdaten gehören niemals in Quellcode, Logs oder Dokumentation.

## Docker und Produktion

Produktionsnahe Umgebung starten:

```bash
docker compose up --build
```

Ein einzelnes Backend-Image wird aus dem Projektstamm gebaut:

```bash
docker build \
  -f services/weather-service/Dockerfile \
  -t warp-weather:local \
  .
```

Frontend-Image:

```bash
docker build \
  -f frontend/Dockerfile \
  --build-arg VITE_BFF_BASE_URL=http://localhost:8080 \
  --build-arg VITE_MAPTILER_PUBLIC_API_KEY=test-key \
  -t warp-frontend:local \
  .
```

Das Frontend wird mehrstufig gebaut und anschließend durch `frontend/server.mjs` mit SPA-Fallback ausgeliefert.

## Projektdump erstellen

### macOS, Linux, WSL oder Git Bash

```bash
./scripts/create-project-dump.sh
```

### Windows PowerShell

```powershell
.\scripts\create-project-dump.ps1
```

Das Ergebnis wird als `dump.txt` im Projektstamm gespeichert. Obwohl bekannte sensible Dateien ausgeschlossen werden, muss der Dump vor der Weitergabe manuell auf Zugangsdaten und personenbezogene Informationen geprüft werden.

## Sicherheitsregeln

- Browserzugriffe ausschließlich über den BFF
- interne Services nicht öffentlich aus dem Frontend aufrufen
- stabile eigene Fehlercodes statt interner Fehlermeldungen
- keine Stacktraces an Clients ausgeben
- Eingaben validieren und geschützte Operationen autorisieren
- Besitzprüfung bei benutzerbezogenen Daten
- externe API-Schlüssel möglichst serverseitig halten
- keine echten externen APIs, kostenpflichtigen Dienste oder echten E-Mail-Versand in automatisierten Tests

## Weiterführende Dokumentation

- [`README-DEV-WATCH.md`](README-DEV-WATCH.md): Docker-Entwicklungsmodus und Hot Reload
- [`frontend/README.md`](frontend/README.md): Frontend, Routen und TypeScript
- [`contracts/graphql/README.md`](contracts/graphql/README.md): GraphQL-Konventionen und Fehlerbehandlung
