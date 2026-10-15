# WaRP-Frontend

Das WaRP-Frontend ist die browserbasierte Benutzeroberfläche von **WaRP (Weather-aware Route Planning)**. Die Anwendung wird mit React, TypeScript und Vite entwickelt und kommuniziert ausschließlich über den BFF-Service mit dem Backend.

Interne Microservices werden nicht direkt aus dem Browser angesprochen. GraphQL-Anfragen gehen an `/graphql`, REST-Anfragen an `/api` beziehungsweise an die dafür vorgesehenen BFF-Endpunkte.

## Technischer Überblick

- React 19
- TypeScript mit aktivem Strict Mode
- Vite
- React Router
- Apollo Client für GraphQL
- Leaflet und React Leaflet für die Kartendarstellung
- CSS Modules und globale CSS-Variablen
- Node.js-basierter Produktionsserver
- Docker-Unterstützung für Entwicklung und Produktion

## Voraussetzungen

Für die lokale Entwicklung werden benötigt:

- Node.js 22.22 oder neuer
- npm
- ein erreichbarer BFF-Service für Status- und API-Anfragen
- ein öffentlicher MapTiler-API-Key für die Kartenansicht

> Die im Projekt verwendete React-Router-Version setzt Node.js 22.22 oder neuer voraus.

## Installation

Vom Projektstamm aus:

```bash
cd frontend
npm ci
```

`npm ci` installiert exakt die in `package-lock.json` festgelegten Abhängigkeiten.

## Umgebungsvariablen

Als Vorlage steht `.env.example` zur Verfügung:

```bash
cd frontend
cp .env.example .env
```

Unterstützte Variablen:

```dotenv
VITE_MAPTILER_PUBLIC_API_KEY=your_maptiler_api_key
VITE_BFF_BASE_URL=
```

### `VITE_MAPTILER_PUBLIC_API_KEY`

Öffentlicher API-Key für die Kartenkacheln von MapTiler. Ohne diesen Wert zeigt der Routenplaner einen Konfigurationshinweis anstelle der Karte.

Da Variablen mit dem Präfix `VITE_` in das Browser-Bundle übernommen werden, dürfen hier keine geheimen Zugangsdaten hinterlegt werden.

### `VITE_BFF_BASE_URL`

Optionale Basis-URL des BFF. Bleibt der Wert leer, verwendet das Frontend relative URLs wie `/graphql` und `/api/v1/system/health`.

Für die lokale Entwicklung ist eine leere Basis-URL empfohlen, da Vite `/graphql` und `/api` an den lokalen BFF weiterleitet.

Beispiel für einen separat erreichbaren BFF:

```dotenv
VITE_BFF_BASE_URL=http://localhost:8080
```

## Lokale Entwicklung

Frontend starten:

```bash
cd frontend
npm run dev
```

Die Anwendung ist anschließend erreichbar unter:

```text
http://localhost:5173
```

Die Standardkonfiguration in `vite.config.ts` leitet folgende Pfade an `http://localhost:8080` weiter:

```text
/graphql
/api
```

Der BFF muss dafür lokal auf Port `8080` erreichbar sein.

## Verfügbare npm-Scripts

### Entwicklungsserver

```bash
npm run dev
```

Startet den Vite-Entwicklungsserver.

### TypeScript-Prüfung

```bash
npm run typecheck
```

Prüft die Anwendung und die Vite-Konfigurationen mit TypeScript, ohne Dateien zu erzeugen.

### Produktions-Build

```bash
npm run build
```

Führt zuerst die TypeScript-Prüfung aus und erstellt danach den Produktions-Build im Verzeichnis `dist`.

### Vollständige Frontend-Prüfung

```bash
npm run check
```

Führt `typecheck` und anschließend den vollständigen Build aus.

### Produktions-Build lokal ansehen

```bash
npm run preview
```

Startet die Vite-Vorschau für einen zuvor erzeugten Build.

## Frontend-Routen

### `/`

Zeigt aktuell das REST-basierte Service-Dashboard. Die Seite fragt den Systemzustand alle fünf Sekunden über folgenden BFF-Endpunkt ab:

```text
GET /api/v1/system/health
```

Angezeigt werden:

- Gesamtzustand der Services
- Anzahl erreichbarer Services
- Status jedes Services
- Erreichbarkeit
- Antwortzeit
- Zeitpunkt der letzten Prüfung

Zusätzlich kann die Aktualisierung manuell ausgelöst werden.

### `/system-status`

Zeigt das GraphQL-basierte Systemstatus-Dashboard. Die Seite führt die Query `SystemHealth` über den BFF-Endpunkt aus:

```text
POST /graphql
```

Angezeigt werden:

- Gesamtstatus des Systems
- Anzahl erreichbarer Services
- Status jedes einzelnen Services
- Erreichbarkeit jedes Services
- Antwortzeit jedes Services
- Zeitpunkt der letzten Prüfung

### `/route-planner`

Zeigt den aktuellen Stand des Routenplaners mit einer Leaflet-Karte. Die Kartenkacheln werden über MapTiler geladen.

Für die Darstellung muss `VITE_MAPTILER_PUBLIC_API_KEY` gesetzt sein. Die eigentliche wetterabhängige Routenplanung ist noch nicht implementiert.

### `/login`

Zeigt eine vorläufige Testanmeldung. Die Anmeldung ist noch nicht mit dem Identity-Service verbunden.

Der aktuelle Authentifizierungsstatus wird ausschließlich zu Entwicklungszwecken im `localStorage` des Browsers gespeichert:

```text
warp-user-authenticated
```

Diese Testanmeldung ist keine produktive Authentifizierung.

### `/profile`

Geschützte Platzhalterseite für zukünftige Profildaten. Nicht angemeldete Benutzer werden zu `/login` weitergeleitet und nach der Testanmeldung zur ursprünglich angeforderten Seite zurückgeführt.

### `/saved-routes`

Geschützte Platzhalterseite für zukünftig gespeicherte Routen. Nicht angemeldete Benutzer werden zu `/login` weitergeleitet.

### Unbekannte Routen

Alle nicht definierten Routen werden über die Route `*` abgefangen. Die 404-Seite zeigt:

- den angefragten Pfad
- einen Link zur Startseite
- einen Link zum Systemstatus

> Eine Route `/status` ist im aktuellen Frontend nicht definiert.

## Anwendungslayout

Alle regulären Seiten werden innerhalb eines gemeinsamen Anwendungslayouts dargestellt. Das Layout enthält:

- WaRP-Anwendungskennung
- responsive Hauptnavigation
- Inhaltsbereich für die aktuelle Route
- Anwendungsfooter

Die aktive Route wird in der Navigation hervorgehoben. Abhängig vom lokalen Test-Anmeldestatus werden außerdem die Links zu Profil und gespeicherten Routen oder der Link zur Anmeldung angezeigt.

## BFF- und GraphQL-Anbindung

Der Apollo Client befindet sich unter:

```text
frontend/src/api/apolloClient.ts
```

Die typisierte GraphQL-Query für den Systemstatus befindet sich unter:

```text
frontend/src/api/systemHealthQuery.ts
```

Die gemeinsamen TypeScript-Typen für System- und Servicezustände befinden sich unter:

```text
frontend/src/types/systemHealth.ts
```

Die verbindliche GraphQL-Schemadefinition des Gesamtprojekts liegt unter:

```text
contracts/graphql/schema.graphqls
```

Konventionen, Fehlercodes und Transportregeln sind dokumentiert unter:

```text
contracts/graphql/README.md
```

Der Apollo Client sendet Cookies mit und verwendet für Antworten den Accept-Header:

```text
application/graphql-response+json
```

## Projektstruktur

```text
frontend/
├── src/
│   ├── api/                 # Apollo Client und GraphQL-Queries
│   ├── app/                 # Einstiegspunkt, Layout und Routing
│   ├── components/          # Wiederverwendbare UI- und Routing-Komponenten
│   ├── features/            # Fachliche Frontend-Features
│   ├── pages/               # Routenseiten
│   ├── styles/              # Globale Styles und Designvariablen
│   ├── types/               # Gemeinsame TypeScript-Typen
│   └── vite-env.d.ts        # Typisierung der Vite-Umgebungsvariablen
├── Dockerfile               # Produktions-Build und Produktionsserver
├── Dockerfile.dev           # Entwicklungscontainer
├── server.mjs               # Statischer Produktionsserver mit SPA-Fallback
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── vite.config.ts           # Lokale Vite-Konfiguration
└── vite.config.dev.ts       # Vite-Konfiguration für Docker-Entwicklung
```

## Docker-Entwicklung

Der Entwicklungscontainer verwendet `Dockerfile.dev` und startet Vite mit `vite.config.dev.ts` auf Port `5173`.

Die BFF-Zieladresse kann im Container über folgende Variable gesetzt werden:

```dotenv
VITE_BFF_PROXY_TARGET=http://bff-service:8080
```

Ohne Angabe wird standardmäßig `http://bff-service:8080` verwendet.

Optional kann Dateisystem-Polling aktiviert werden:

```dotenv
VITE_USE_POLLING=true
```

Das Entwicklungs-Entrypoint-Skript synchronisiert `node_modules` automatisch mit `package-lock.json`, wenn Abhängigkeiten fehlen oder sich die Lockdatei geändert hat.

Der konkrete Start erfolgt normalerweise über die Docker-Compose-Konfiguration des Gesamtprojekts. Bei Änderungen an Abhängigkeiten oder Docker-Konfigurationen sollte das Frontend-Image neu gebaut werden.

## Produktions-Build und Server

Das Produktions-Dockerfile arbeitet mehrstufig:

1. Installation der Abhängigkeiten mit `npm ci`
2. TypeScript-Prüfung und Vite-Build über `npm run build`
3. Übernahme des erzeugten `dist`-Verzeichnisses in ein schlankes Node.js-Runtime-Image
4. Start des statischen Servers über `server.mjs`

Der Produktionsserver:

- verwendet den Port aus `PORT`, standardmäßig `8080`
- bindet an `0.0.0.0`
- liefert statische Dateien aus `dist` aus
- verwendet für unbekannte Pfade `index.html` als SPA-Fallback
- stellt einen Health-Endpunkt bereit

```text
GET /health
```

Erfolgreiche Antwort:

```json
{
  "status": "UP"
}
```

Build-Variablen können beim Erstellen des Images übergeben werden:

```text
VITE_BFF_BASE_URL
VITE_MAPTILER_PUBLIC_API_KEY
```

## Manuelle Prüfung

Nach dem Start sollten mindestens folgende Adressen geprüft werden:

```text
http://localhost:5173/
http://localhost:5173/system-status
http://localhost:5173/route-planner
http://localhost:5173/login
http://localhost:5173/profile
http://localhost:5173/saved-routes
http://localhost:5173/unbekannte-route
```

Erwartetes Verhalten:

1. `/` zeigt das REST-basierte Service-Dashboard.
2. `/system-status` zeigt das GraphQL-basierte Service-Dashboard.
3. `/route-planner` zeigt bei konfiguriertem MapTiler-Key die Karte.
4. `/login` ermöglicht eine vorläufige lokale Testanmeldung.
5. `/profile` und `/saved-routes` leiten ohne Testanmeldung zu `/login` weiter.
6. Nach der Testanmeldung wird zur ursprünglich angeforderten geschützten Route zurückgeleitet.
7. `/unbekannte-route` zeigt die 404-Seite.
8. Die Navigation markiert die aktive Route.
9. Das direkte Neuladen einer Client-Route funktioniert im Produktionsserver durch den SPA-Fallback.
10. Fehler des BFF oder der Kartenkacheln werden in der Oberfläche angezeigt.

## Empfohlener Prüfablauf

```bash
cd frontend
npm ci
npm run check
npm run dev
```

Der aktuelle Projektstand enthält keine separate automatisierte Test-Suite und keinen Lint-Befehl. Die verbindlichen automatisierten Frontend-Prüfungen sind daher derzeit die TypeScript-Prüfung und der Produktions-Build.
