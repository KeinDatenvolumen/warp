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

Zeigt das Anmeldeformular mit E-Mail-Adresse und Passwort. Die Anmeldung läuft über den BFF (`POST /api/v1/sessions`). Details siehe Abschnitt [Anmeldung und Sitzungsstatus](#anmeldung-und-sitzungsstatus).

Bereits angemeldete Benutzer werden von `/login` automatisch weitergeleitet.

### `/profile`

Geschützte Platzhalterseite für zukünftige Profildaten. Nicht angemeldete Benutzer werden zu `/login` weitergeleitet und nach der Anmeldung zur ursprünglich angeforderten Seite zurückgeführt. Während der Sitzungsprüfung beim Start wird kein geschützter Inhalt angezeigt.

### `/saved-routes`

Geschützte Platzhalterseite für zukünftig gespeicherte Routen. Nicht angemeldete Benutzer werden zu `/login` weitergeleitet.

### Unbekannte Routen

Alle nicht definierten Routen werden über die Route `*` abgefangen. Die 404-Seite zeigt:

- den angefragten Pfad
- einen Link zur Startseite
- einen Link zum Systemstatus

> Eine Route `/status` ist im aktuellen Frontend nicht definiert.

### Farbprofile und Modus

Es gibt drei Farbprofile (`profile-1` bis `profile-3`) in hell und dunkel. Auswahl und Modus (Hell, Dunkel, Wie Gerät) liegen im `localStorage` unter `warp.theme`. Der `ThemeProvider` setzt `data-color-profile` und `data-color-mode` auf `<html>`. Die Farben selbst gehören in `src/styles/themes.css` (sechs Blöcke, derzeit leer, es gelten die Werte aus `variables.css`). Komponenten sollen Farben nur über die CSS-Variablen aus `variables.css` verwenden.

## Anwendungslayout

Alle regulären Seiten werden innerhalb eines gemeinsamen Anwendungslayouts dargestellt. Das Layout enthält:

- WaRP-Anwendungskennung
- responsive Hauptnavigation
- Inhaltsbereich für die aktuelle Route
- Anwendungsfooter

Die aktive Route wird in der Navigation hervorgehoben. Abhängig vom Sitzungsstatus werden außerdem die Links zu Profil und gespeicherten Routen samt Abmelden-Schaltfläche (angemeldet) oder der Link zur Anmeldung (abgemeldet) angezeigt. Während der Sitzungsprüfung beim Start wird weder das eine noch das andere angezeigt.

## Anmeldung und Sitzungsstatus

### Ablauf

1. Beim Start der Anwendung fragt der `AuthProvider` die Sitzung über die GraphQL-Query `currentSession` ab.
2. Die Anmeldung erfolgt über `POST /api/v1/sessions`. Der BFF setzt das Session-Cookie (`HttpOnly`, `Secure`, `SameSite=Lax`).
3. Danach wird `currentSession` erneut abgefragt. Erst diese Antwort setzt den Status auf `authenticated`.
4. Die Abmeldung erfolgt nach einer Rückfrage („Wirklich abmelden?“) über `DELETE /api/v1/sessions/current`. Danach werden Status und Apollo-Cache zurückgesetzt und zur Startseite weitergeleitet.

### Authentifizierungsstatus

Der zentrale Status (`useAuth().status`) hat genau drei Werte:

```text
loading            Sitzung wird geprüft
authenticated      Benutzer ist angemeldet
unauthenticated    Benutzer ist nicht angemeldet
```

Das Session-Cookie ist `HttpOnly` und wird vom Frontend nie gelesen oder verändert. Maßgeblich ist ausschließlich die Antwort des Backends. Es wird nichts über den Anmeldestatus in `localStorage` oder `sessionStorage` gespeichert. Das Passwort liegt nur im Zustand der Anmeldeseite und wird nach einem Fehlversuch geleert.

Meldet das Backend bei einer GraphQL-Operation `SESSION_INVALID`, wechselt der Status automatisch zu `unauthenticated` (`sessionInvalidLink` in `src/api/apolloClient.ts`).

### CSRF

`POST /api/v1/sessions` und `POST /api/v1/users` sind von CSRF ausgenommen. Für alle anderen zustandsverändernden REST-Requests (derzeit `DELETE /api/v1/sessions/current`) lädt `src/api/restClient.ts` den Token über `GET /api/v1/csrf`, hält ihn nur im Arbeitsspeicher und sendet ihn im vom BFF genannten Header (`X-XSRF-TOKEN`). Bei einem `403` wird der Token einmal erneuert und der Request wiederholt.

GraphQL-Mutationen benötigen ebenfalls den CSRF-Token. Der Apollo-Link `src/api/csrfLink.ts` lädt ihn über `getCsrfToken` aus dem REST-Client, sendet ihn nur bei Mutationen (Queries bleiben unverändert) und wiederholt bei einem `403` einmal mit erneuertem Token.

### Gespeicherte Routen

| Pfad | Seite | Zugriff |
|---|---|---|
| `/saved-routes` | Liste mit Name, Start, Ziel, Profil und Wetterscore | angemeldet |
| `/saved-routes/:routeId` | Detailansicht mit Karte | angemeldet |

- Die Liste nutzt `savedRoutes(page:)` (20 Einträge pro Seite), die Detailseite `savedRoute(id:)`. Ist die Route nicht vorhanden oder gehört sie einem anderen Benutzer, zeigt die Seite „Diese Route wurde nicht gefunden“.
- Die Karte (`RouteMap`) zeichnet die Abschnitte in den gespeicherten Wetterscore-Farben. Fehlen Abschnittsgeometrien, wird die gesamte Route in einer neutralen Farbe gezeichnet.
- Löschen läuft immer über `ConfirmDialog` („Route wirklich löschen?“) und die Mutation `deleteSavedRoute`.
- Namen werden mit `validateRouteName` geprüft (nicht leer, höchstens 100 Zeichen, wie im Storage-Service).
- Das Formular `SaveRouteForm` ruft `saveRoute` mit `routeCalculationId` und `weatherEvaluationId` auf. Es erscheint im Routenplaner erst, wenn eine berechnete Route samt Wetterbewertung vorliegt.

### Rückfrage bei der Abmeldung

Die Rückfrage verwendet die wiederverwendbare Komponente `src/components/ui/ConfirmDialog/ConfirmDialog.tsx` auf Basis des nativen `<dialog>`-Elements. Der Browser übernimmt Fokus-Falle, Abdunkeln des Hintergrunds und Schließen mit `Esc`. Beim Öffnen liegt der Fokus auf „Abbrechen“, damit ein versehentliches `Enter` nicht abmeldet. Der Dialog kann auch für andere Rückfragen genutzt werden (Titel, Beschreibung und Beschriftung des Bestätigen-Buttons sind Props).

### Fehlermeldungen der Anmeldung

| Ursache | Anzeige |
|---|---|
| `INVALID_CREDENTIALS` (401) | Die E-Mail-Adresse oder das Passwort ist nicht korrekt. (bewusst keinem Feld zugeordnet) |
| `429` | Zu viele Anmeldeversuche, mit Wartezeit aus `Retry-After`. Das Absenden ist bis zum Ablauf gesperrt. |
| alle anderen Fehler | Die Anmeldung konnte nicht abgeschlossen werden. Bitte versuche es erneut. |

### Weiterleitung

Nach der Anmeldung wird zur ursprünglich angefragten geschützten Seite (`location.state.from`) weitergeleitet, sonst zu `DEFAULT_AUTHENTICATED_PATH` (`src/features/auth/authRedirect.ts`). Es werden nur interne Pfade akzeptiert. Nach der Abmeldung wird zur öffentlichen Startseite weitergeleitet.

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
4. `/login` zeigt das Anmeldeformular. Leere Felder und eine ungültige E-Mail-Adresse werden direkt am Feld gemeldet.
5. `/profile` und `/saved-routes` leiten ohne Anmeldung zu `/login` weiter. Beim Neuladen einer geschützten Seite wird kein Inhalt kurzzeitig angezeigt.
6. Nach der Anmeldung wird zur ursprünglich angeforderten geschützten Route zurückgeleitet. Ein Neuladen behält die Anmeldung.
7. Falsche Zugangsdaten zeigen die allgemeine Fehlermeldung.
8. **Abmelden** öffnet die Rückfrage „Wirklich abmelden?“. „Abbrechen“, `Esc` und ein Klick neben das Fenster schließen sie ohne Abmeldung. „Abmelden“ beendet die Sitzung und leitet zur Startseite weiter. Geschützte Seiten sind danach gesperrt.
9. `/unbekannte-route` zeigt die 404-Seite.
10. Die Navigation markiert die aktive Route.
11. Das direkte Neuladen einer Client-Route funktioniert im Produktionsserver durch den SPA-Fallback.
12. Fehler des BFF oder der Kartenkacheln werden in der Oberfläche angezeigt.

## Empfohlener Prüfablauf

```bash
cd frontend
npm ci
npm run check
npm run dev
```

## Frontend-Tests

### Testabhängigkeiten installieren

Im Verzeichnis `frontend` ausführen:

```powershell
npm install
```

Die Tests verwenden Vitest, JSDOM und React Testing Library.

### Tests ausführen

Alle Frontend-Tests einmal ausführen:

```powershell
npm test
```

Tests im Watch-Modus ausführen:

```powershell
npm run test:watch
```

TypeScript prüfen:

```powershell
npm run typecheck
```

Den vollständigen Frontend-Check ausführen:

```powershell
npm run check
```

### Teststruktur

Die gemeinsame Testkonfiguration befindet sich in:

```text
frontend/vite.config.ts
frontend/vite.config.dev.ts
frontend/src/test/setup.ts
```

Die Registrierungstests befinden sich in:

```text
frontend/src/features/registration/registrationValidation.test.ts
frontend/src/features/registration/registrationErrorMapping.test.ts
frontend/src/pages/RegistrationPage.test.tsx
```

Die Tests zu Anmeldung und Sitzungsstatus befinden sich in:

```text
frontend/src/api/restClient.test.ts
frontend/src/api/sessionApi.test.ts
frontend/src/api/apolloClient.test.ts
frontend/src/features/auth/AuthContext.test.tsx
frontend/src/features/auth/authRedirect.test.ts
frontend/src/features/auth/loginErrorMapping.test.ts
frontend/src/features/auth/loginValidation.test.ts
frontend/src/features/auth/sessionEvents.test.ts
frontend/src/features/auth/useCountdown.test.ts
frontend/src/components/routing/ProtectedRoute.test.tsx
frontend/src/pages/LoginPage.test.tsx
frontend/src/app/AppLayout.test.tsx
```

### Neue Tests anlegen

Vitest erkennt Testdateien automatisch, wenn sie nach einem dieser Muster benannt sind:

```text
*.test.ts
*.test.tsx
*.spec.ts
*.spec.tsx
```

Beispiele:

```text
src/features/example/example.test.ts
src/pages/ExamplePage.test.tsx
```

### Aktuelle Testabdeckung der Registrierung

Die Registrierungstests prüfen:

- Pflichtfeldvalidierung für Name, E-Mail-Adresse und Passwort
- formale Prüfung der E-Mail-Adresse
- Mindestlänge des Passworts von zwölf Zeichen
- verständliche Feldfehlermeldungen
- Entfernung von Feldfehlern nach einer Korrektur
- korrekte HTML-Typen und Autocomplete-Attribute
- barrierearme Fehlerzuordnung über `aria-invalid` und `aria-describedby`
- Übergabe gültiger Daten an die Registrierungs-API
- Ladezustand während der Anfrage
- Schutz vor mehrfacher Übermittlung
- Backendfehler `EMAIL_ALREADY_EXISTS`
- serverseitige `fieldErrors`
- allgemeine Fehlermeldungen bei technischen Fehlern
- Anzeige der Erfolgsmeldung des Backends
- Bedienung des Formulars per Tastatur

Der aktuelle Projektstand enthält automatisierte Frontend-Tests mit Vitest, JSDOM und React Testing Library. Ein separater Lint-Befehl ist derzeit nicht konfiguriert.

### Aktuelle Testabdeckung der Anmeldung

Die Tests zu Anmeldung und Sitzungsstatus prüfen:

- Pflichtfeld- und E-Mail-Formatvalidierung, Fokus auf dem ersten fehlerhaften Feld
- Eingabetypen und Autocomplete-Werte (`email`, `current-password`)
- barrierearme Fehlerzuordnung über `aria-invalid`, `aria-describedby` und `role="alert"`
- Aufruf von `POST /api/v1/sessions` mit getrimmter E-Mail-Adresse
- Ladezustand und Schutz vor mehrfachem Absenden
- allgemeine Fehlermeldung bei `INVALID_CREDENTIALS` ohne Feldzuordnung
- Behandlung von `429` mit und ohne `Retry-After`
- technische Fehler und nicht bestätigte Sitzungen
- Sitzungsprüfung beim Start (`loading`, `authenticated`, `unauthenticated`)
- automatischen Wechsel in den abgemeldeten Zustand bei `SESSION_INVALID`
- Schutz geschützter Seiten, auch während der Sitzungsprüfung
- Weiterleitung nach Anmeldung und Abmeldung, nur interne Zielpfade
- Abmeldung inklusive Ladezustand, Fehlerfall und Zurücksetzen des Apollo-Caches
- Rückfrage vor der Abmeldung: Fokus auf „Abbrechen“, Schließen per Button, `Esc` und Hintergrundklick ohne Abmeldung
- CSRF-Token bei `DELETE /api/v1/sessions/current`, einmalige Wiederholung bei `403`
- keine Nutzung von `localStorage`, `sessionStorage` und `document.cookie`
- Bedienung des Formulars per Tastatur