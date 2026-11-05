<!-- [KI-Log Nr. KI-21] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone RabbitMQ Mail-Queue & Broker-Fallback (WARP-036) -->
# WaRP: Weather-aware Route Planning

WaRP ist eine mobile Webanwendung zur wetterabhängigen Planung von Fahrrad-, Motorrad- und Wanderrouten. Geplante Routen werden mit räumlich und zeitlich passenden Wetterdaten verknüpft, über eine 101-stufige perzeptuelle Skala bewertet, per KI analysiert und benutzergebunden gespeichert.

> **Projektstatus:** Alle fachlichen und architektonischen Kernfunktionen (Composable Architecture / MACH, SPA & MPA Touchpoints, GraphQL & REST, Authentifizierung & Sitzungsverwaltung, asynchrone Routenberechnung, Wetterbewertung, Abfahrtszeit-Optimierung, KI-Wetterberater mit MCP-Server, RabbitMQ-Messaging sowie Caffeine In-Memory-Caching) sind vollständig und produktiv umgesetzt.

## Aktueller Funktionsumfang

Produktiv umgesetzt sind:

- **Composable Microservice-Architektur (MACH):** Gradle-Multi-Project-Build mit Java 21 und fünf getrennten Spring-Boot-Services (`bff-service`, `identity-service`, `routing-service`, `weather-service`, `storage-service`)
- **Zwei unabhängige Touchpoints:**
  - **Single-Page Application (SPA):** React 19, TypeScript (Strict Mode), Vite, Apollo Client, React Router und interaktive Leaflet-Karte
  - **Multi-Page Application (MPA):** Serverseitig gerenderte Express-/EJS-Anwendung mit GraphQL- und REST-Anbindung an das BFF
- **Hybride API-Schicht (GraphQL & REST):** Öffentlicher GraphQL- und REST-Zugang über das zustandslose BFF; Contract-First mit `contracts/graphql/`, `contracts/openapi/` und `contracts/asyncapi/`
- **Authentifizierung & Kontoverwaltung:** Produktive Registrierung, BCrypt-Passwort-Hashing, HttpOnly-Sitzungs-Cookies, CSRF-Schutz, Rate-Limiting, E-Mail-Verifikation, Passwort-Zurücksetzung sowie Passwort-/E-Mail-Änderung im Profil
- **Geocodierung & Asynchrone Routenberechnung:** Ortssuche über MapTiler Geocoding API und asynchrone Routenberechnung über OpenRouteService mit zentralen Aktivitätsprofilen (`BICYCLE`, `MOTORCYCLE`, `HIKING`) und Idempotenz-Schutz
- **Räumlich-zeitliche Wetterauswertung & Optimierung:** Verknüpfung der Routensegmente mit Open-Meteo-Prognosen, 101-stufige Farbskala, Meteogramm-Dock, Gewitter-Deckelung (`thunderstormCap`) und interaktiver Abfahrtszeit-Optimierer
- **KI-Wetteranalyse & MCP-Server:** Integrierter Model Context Protocol (MCP) JSON-RPC 2.0 Server im `weather-service` in Kombination mit Google Gemini für taktische Outdoor-Wetterberatung
- **Infrastruktur-Optimierungen:**
  - **In-Memory Caching (Caffeine):** Schnelles Caching für Sitzungs-Validierungen (`identity-service`, TTL 5 Min.) und Geocoding-Suchergebnisse (`routing-service`, TTL 24 Std.)
  - **Message Queue (RabbitMQ):** Asynchrone Entkopplung des E-Mail-Versands über `warp.mail.send` mit Retry-Strategie, Dead-Letter-Queue (`warp.mail.send.dlq`) und automatischem Direkt-Fallback
- **Database-per-Service:** Vier isolierte PostgreSQL-18-Datenbanken mit Flyway-Migrationen und Hibernate-Schemavalidierung (`ddl-auto=validate`)
- **DevOps & Cloud-Deployment:** Lokale Docker-Entwicklungsumgebung mit Hot Reload, Mailpit-Testserver sowie automatisierte CI/CD-Pipelines (GitHub Actions) und Container-Deployments auf Heroku

## Architektur

```text
Browser
  ├─> React-/TypeScript-SPA (Port 5173) ──┐
  │     └─> Direktabruf Kartenkacheln ────┼─> MapTiler / OpenStreetMap CDN
  └─> Node.js/Express-MPA   (Port 8085) ──┤
                                          ▼
                                BFF-Service (Port 8080, Stateless)
                                  ├─> Identity-Service (8081) ──> Caffeine Cache + PostgreSQL + RabbitMQ -> Mailpit/SMTP
                                  ├─> Routing-Service  (8082) ──> Caffeine Cache + PostgreSQL + MapTiler + OpenRouteService
                                  ├─> Weather-Service  (8083) ──> MCP Server + PostgreSQL + Open-Meteo + Google Gemini AI
                                  └─> Storage-Service  (8084) ──> PostgreSQL
```

Die Frontends (SPA und MPA) kommunizieren für alle fachlichen Daten ausschließlich mit dem BFF. Interne Services werden niemals direkt aus dem Browser angesprochen. Einzige bewusste und begründete Ausnahme ist das Laden der Raster-Kartenkacheln (MapTiler / OpenStreetMap Tiles) durch Leaflet direkt im Browser, um das BFF nicht mit binärem Karten-Streaming zu belasten und Browser-/CDN-Caching optimal zu nutzen.

### Services

| Service | Aufgabe | Port | Datenbank / Speicher |
|---|---|---:|---|
| `bff-service` | öffentlicher Einstiegspunkt, REST und GraphQL | `8080` | keine (stateless) |
| `identity-service` | Benutzer, Anmeldung, Sessions, E-Mail-Verifikation | `8081` | `warp_identity` + Caffeine Cache (`session-lookups`) |
| `routing-service` | Geocodierung, Aktivitätsprofile, Routenberechnung | `8082` | `warp_routing` + Caffeine Cache (`geocoding-results`) |
| `weather-service` | Wetterdaten, 101-Stufen-Score, Zeitoptimierung, MCP & Gemini AI | `8083` | `warp_weather` |
| `storage-service` | gespeicherte Routen, Favoriten und persistente Fachdaten | `8084` | `warp_storage` |
| Frontend (SPA) | interaktive Weboberfläche (React, TypeScript & Vite) | `5173` | keine |
| MPA | Multi-Page Application (Node.js, Express, EJS & GraphQL) | `8085` | keine |
| Mailpit UI | lokale E-Mail-Ansicht | `8025` | lokales Volume |
| Mailpit SMTP | lokaler SMTP-Eingang | `1025` | lokales Volume |
| RabbitMQ | Message Queue für den E-Mail-Versand (UI auf `15672`) | `5672` | lokales Volume |

Die PostgreSQL-Instanzen sind in der Docker-Entwicklung zusätzlich über die Host-Ports `5434` bis `5437` erreichbar.

## Technologieübersicht

### Backend

- Java 21
- Spring Boot 4.1.1
- Gradle 9.8.0 über den Gradle Wrapper
- Spring GraphQL & Spring Security im BFF
- Spring Cache & Caffeine (`com.github.ben-manes.caffeine:caffeine`) für In-Memory-Caching
- Spring AMQP / RabbitMQ für asynchrone Nachrichtenverarbeitung
- Model Context Protocol (MCP, JSON-RPC 2.0) & Google Gemini API im Weather-Service
- PostgreSQL 18 in der Docker-Entwicklung
- Flyway für Datenbankmigrationen
- Hibernate mit `ddl-auto=validate`

### Frontends (SPA & MPA)

- **SPA (`frontend/`):** React 19.3, TypeScript 7 (Strict Mode), Vite 8, React Router 8, Apollo Client 4, Leaflet & React Leaflet, CSS Modules, Vitest & React Testing Library
- **MPA (`mpa/`):** Node.js 22, Express, TypeScript, EJS-Templates, Helmet Security Headers, GraphQL & REST Client, Vitest & Supertest

Für die lokale Frontend-Entwicklung wird **Node.js 22.22 oder neuer** empfohlen.

## Projektstruktur

```text
warp/
├── .github/workflows/              # Prüf- und Heroku-Deployment-Workflows
├── contracts/
│   ├── asyncapi/                   # AsyncAPI-Vertrag für RabbitMQ Mail-Events
│   ├── examples/                   # Beispielantworten externer APIs
│   ├── graphql/                    # öffentlicher GraphQL-Vertrag (schema.graphqls)
│   └── openapi/                    # OpenAPI 3.1 REST-Verträge aller Services
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
MPA:         http://localhost:8085
BFF:         http://localhost:8080
Identity:    http://localhost:8081
Routing:     http://localhost:8082
Weather:     http://localhost:8083
Storage:     http://localhost:8084
Mailpit UI:  http://localhost:8025
RabbitMQ UI: http://localhost:15672
Swagger UI:  http://localhost:8088
```

### Interaktive API-Dokumentation (Swagger UI)

Die OpenAPI-3.1-Spezifikationen aller fünf Microservices werden über **Swagger UI** interaktiv im Browser bereitgestellt:

- **Im Docker-Setup:** Direkt erreichbar unter [`http://localhost:8088`](http://localhost:8088). Über das Dropdown-Menü in der oberen Leiste kann zwischen allen Services gewechselt werden:
  1. **BFF Service** (Öffentliche REST-Endpunkte, CSRF, Health, Optimierung)
  2. **Identity Service** (Interne User-, Session- & Auth-Verwaltung)
  3. **Routing Service** (Aktivitätsprofile, MapTiler-Geocoding, async Routen-Dispatch)
  4. **Weather Service & MCP** (Open-Meteo-Wetter, Gemini-KI, MCP-Server)
  5. **Storage Service** (Routen-, Favoriten- & Evaluation-Persistenz)
- **Ohne Docker / Offline:** Die Datei [`contracts/openapi/index.html`](contracts/openapi/index.html) oder [`0300_Praesentationen/warp-api-docs.html`](0300_Praesentationen/warp-api-docs.html) kann direkt im Browser geöffnet oder via lokalem HTTP-Server (`npx serve contracts/openapi` oder Python) ausgeliefert werden.
- **Interaktives Testen („Try it out“):** Der BFF erlaubt CORS-Anfragen von `http://localhost:8088`, sodass Endpunkte direkt aus dem Swagger UI heraus gegen die laufende Instanz getestet werden können.

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

### Frontend (SPA) installieren und vollständig prüfen

```bash
cd frontend
npm ci
npm run check
npm test
```

Verfügbare Frontend-Scripts:

```bash
npm run dev        # Vite-Entwicklungsserver
npm run typecheck  # TypeScript-Prüfung ohne Ausgabe
npm run build      # TypeScript-Prüfung und Produktions-Build
npm run check      # typecheck und build
npm test           # Automatisierte Frontend-Tests (Vitest & React Testing Library)
npm run preview    # lokalen Produktions-Build anzeigen
```

### Multi-Page Application (MPA) installieren und prüfen

```bash
cd mpa
npm ci
npm test
npm run build
```

## Frontend-Routen (SPA & MPA)

### Single-Page Application (`frontend/`, Port `5173`)

| Route | Beschreibung |
|---|---|
| `/` | Automatische Weiterleitung auf `/route-planner` |
| `/route-planner` | Interaktive Leaflet-Karte, Ortssuche, Routenberechnung, 101-stufiger Wetterscore, Meteogramm-Dock, Abfahrtszeit-Optimierer & KI-Wetterberater |
| `/login` | Produktive Anmeldung über `POST /api/v1/sessions` (HttpOnly-Cookie) |
| `/register` | Benutzerregistrierung über `POST /api/v1/users` |
| `/verify-email` | Bestätigung der E-Mail-Adresse per Token |
| `/forgot-password` | Anfordern eines Links zum Zurücksetzen des Passworts |
| `/reset-password` | Festlegen eines neuen Passworts per Reset-Token |
| `/profile` | Geschützte Profilseite (Kontodaten, Passwort-/E-Mail-Änderung, Farbschema & Dark-Mode) |
| `/saved-routes` | Geschützte Übersicht gespeicherter Routen mit Favoriten-Filter, Paginierung, Umbenennen und Löschen |
| `/saved-routes/:routeId` | Geschützte Detailansicht einer gespeicherten Route samt Karte und Wettersegmenten |
| `/system-health` | REST-basiertes Service-Dashboard mit automatischer Aktualisierung |
| `/system-status` | GraphQL-basiertes Systemstatus-Dashboard |
| `*` | 404-Seite mit angefragtem Pfad |

### Multi-Page Application (`mpa/`, Port `8085`)

| Route | Beschreibung |
|---|---|
| `/` | Serverseitig gerenderte Übersichtsseite mit Kontostatus und per GraphQL geladenen Aktivitätsprofilen |
| `/login` | Klassisches Formular-Login mit BFF-Session-Cookie |
| `/register` | Serverseitig verarbeitete Benutzerregistrierung |
| `/profile` | Geschützte Profilseite (lädt `currentUser` und `currentSession` per GraphQL vom BFF) |
| `/logout` | Sitzungsinvalidierung am BFF und Cookie-Bereinigung |
| `/health` | Technischer Healthcheck-Endpunkt |

## BFF, REST, GraphQL und AsyncAPI

Die API-Verträge sind nach dem Contract-First-Prinzip unter `contracts/` hinterlegt:

- **GraphQL-Vertrag (`POST /graphql`):** [`contracts/graphql/schema.graphqls`](contracts/graphql/schema.graphqls) sowie [`contracts/graphql/README.md`](contracts/graphql/README.md)
- **Öffentlicher BFF REST-Vertrag:** [`contracts/openapi/bff-service.yaml`](contracts/openapi/bff-service.yaml) und [`contracts/openapi/bff-account.yaml`](contracts/openapi/bff-account.yaml)
- **Interne Microservice REST-/MCP-Verträge:**
  - [`contracts/openapi/identity-service.yaml`](contracts/openapi/identity-service.yaml)
  - [`contracts/openapi/routing-service.yaml`](contracts/openapi/routing-service.yaml)
  - [`contracts/openapi/weather-service.yaml`](contracts/openapi/weather-service.yaml)
  - [`contracts/openapi/storage-service.yaml`](contracts/openapi/storage-service.yaml)
- **AsyncAPI-Vertrag (RabbitMQ):** [`contracts/asyncapi/mail-events.yaml`](contracts/asyncapi/mail-events.yaml)

## Healthchecks

Die Spring-Boot-Services stellen ihre Actuator-Probes unter dem internen Basispfad `/internal` bereit:

```text
http://localhost:8080/internal/health/readiness
http://localhost:8081/internal/health/readiness
http://localhost:8082/internal/health/readiness
http://localhost:8083/internal/health/readiness
http://localhost:8084/internal/health/readiness
```

Ein erfolgreicher Readinesscheck liefert:

```json
{
  "status": "UP"
}
```

Die Produktionsserver von SPA (`frontend`) und MPA (`mpa`) stellen jeweils bereit:

```text
GET /health
```

## In-Memory Caching (Caffeine)

Zur Optimierung der Antwortzeiten, Reduktion der Datenbanklast und Schonung externer API-Quotas setzen `identity-service` und `routing-service` lokale **Caffeine In-Memory-Caches** (`spring-boot-starter-cache` + `com.github.ben-manes.caffeine:caffeine`) ein:

1. **Identity-Service (`session-lookups`):**
   - Speichert unveränderliche `CachedSession`-Snapshots validierter Sitzungs-Tokens (`maximumSize=10000`, `expireAfterWrite=5m`, `sync=true`).
   - Bei einer Abmeldung (`invalidateSession`) wird der Eintrag sofort per `@CacheEvict` aus dem Cache entfernt.
2. **Routing-Service (`geocoding-results`):**
   - Speichert normalisierte (`trim()` + `toLowerCase(Locale.ROOT)`) Ortssuchergebnisse (`Place`-Records) von MapTiler (`maximumSize=5000`, `expireAfterWrite=24h`, `sync=true`).

## KI-Wetteranalyse & Model Context Protocol (MCP)

Der `weather-service` implementiert unter `/internal/v1/weather/mcp` einen standardkonformen **Model Context Protocol (MCP) JSON-RPC 2.0 Server** (`initialize`, `tools/list`, `tools/call`) mit spezialisierten Wetter-Analyse-Tools (`WeatherMcpTools`). In Kombination mit der **Google Gemini API** (`POST /api/v1/routes/weather-ai-analysis`) werden für berechnete Routen Gefahrenstellen, verdeckte Wetterparameter, Ausrüstungsempfehlungen und optimale Startzeitfenster ausgewertet.

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
- [`frontend/README.md`](frontend/README.md): Single-Page Application (SPA), Routen, Tests und TypeScript
- [`mpa/README.md`](mpa/README.md): Multi-Page Application (MPA), Express/EJS und GraphQL-Anbindung
- [`contracts/graphql/README.md`](contracts/graphql/README.md): GraphQL-Konventionen, Operationen und Fehlerbehandlung
- [`contracts/openapi/`](contracts/openapi/): OpenAPI 3.1 REST- und MCP-Verträge aller Services (`bff-service.yaml`, `identity-service.yaml`, `routing-service.yaml`, `weather-service.yaml`, `storage-service.yaml`) sowie interaktives [`index.html`](contracts/openapi/index.html) (Swagger UI)
- [`contracts/asyncapi/mail-events.yaml`](contracts/asyncapi/mail-events.yaml): AsyncAPI 3.0 Vertrag für RabbitMQ Mail-Aufträge
