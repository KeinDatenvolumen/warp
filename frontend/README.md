# WaRP-Frontend

Das WaRP-Frontend ist eine React-Anwendung, die mit Vite gebaut wird.

Das Frontend kommuniziert ausschließlich über den BFF-Service mit dem Backend. GraphQL-Anfragen werden an den BFF-Endpunkt `/graphql` gesendet. Interne Services werden niemals direkt aus dem Browser angesprochen.

## Anwendungslayout

Alle regulären Seiten werden innerhalb des gemeinsamen Anwendungslayouts dargestellt.

Das Layout enthält:

- WaRP-Anwendungskennung
- Hauptnavigation
- Inhaltsbereich für die aktuelle Route
- Anwendungsfooter

Die aktuell geöffnete Route wird in der Navigation hervorgehoben.

## Frontend-Routen

### `/`

Zeigt die Startseite der Anwendung.

Die Startseite gibt einen Überblick über WaRP und verweist auf bereits verfügbare sowie geplante Funktionen.

### `/system-status`

Zeigt das technische Service-Dashboard.

Die Seite ruft die GraphQL-Query `SystemHealth` beim BFF auf und stellt folgende Informationen dar:

- Gesamtstatus des Systems
- Anzahl erreichbarer Services
- Status jedes einzelnen Services
- Erreichbarkeit jedes Services
- Antwortzeit jedes Services
- Zeitpunkt der letzten Prüfung

### `/status`

Leitet innerhalb der Client-Anwendung auf `/system-status` weiter.

Die kanonische Route des Dashboards ist `/system-status`.

### Unbekannte Routen

Alle unbekannten Frontend-Routen werden durch die Route `*` abgefangen.

Das Frontend zeigt eine 404-Seite mit:

- dem angefragten Pfad
- einem Link zur Startseite
- einem Link zum Systemstatus

## GraphQL-Anbindung

Der Apollo Client befindet sich unter:

```text
frontend/src/api/apolloClient.js
```

Der GraphQL-Endpunkt lautet:

```text
POST /graphql
```

Die Query für das Service-Dashboard befindet sich unter:

```text
frontend/src/api/systemHealthQuery.js
```

Die verbindliche GraphQL-Schemadefinition liegt unter:

```text
contracts/graphql/schema.graphqls
```

Konventionen, Fehlercodes und Transportregeln sind dokumentiert unter:

```text
contracts/graphql/README.md
```

## Lokale Entwicklung

Das Frontend kann separat gestartet werden:

```bash
cd frontend
npm ci
npm run dev
```

Anschließend ist es unter folgender Adresse erreichbar:

```text
http://localhost:5173
```

Der lokale Vite-Entwicklungsserver leitet Anfragen an `/graphql` und `/api` an den BFF weiter.

## Build prüfen

```bash
cd frontend
npm run build
```

Der Build muss ohne Fehler abgeschlossen werden.

## Manuelle Routenpruefung

Folgende Adressen müssen geprueft werden:

```text
http://localhost:5173/
http://localhost:5173/system-status
http://localhost:5173/status
http://localhost:5173/unbekannte-route
```

Erwartetes Verhalten:

1. `/` zeigt die Startseite.
2. `/system-status` zeigt das Service-Dashboard.
3. `/status` leitet auf `/system-status` weiter.
4. `/unbekannte-route` zeigt die 404-Seite.
5. Die Navigation markiert die aktive Route.
6. Ein erneutes Laden auf `/system-status` funktioniert.
