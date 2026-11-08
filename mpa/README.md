<!-- [KI-Log Nr. KI-228] (7235e0b) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(mpa): implement basic multi-page application with auth, profile, and GraphQL integration (WARP-034) -->
<!-- [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034) -->
# WaRP Multi-Page Application (MPA)

Die **WaRP Multi-Page Application (MPA)** ist eine leichtgewichtige, serverseitig gerenderte Webanwendung für den Weather-Aware Route Planner (WaRP). Sie ergänzt die bestehende Single-Page Application (SPA) und bietet klassische Multi-Page-Navigation, Server-Side Rendering (SSR) sowie direkte GraphQL- und REST-Anbindung an das Backend-for-Frontend (BFF).

## Funktionen

- **Übersichtsseite (`/`)**: Begrüßung, Kurzübersicht zum Kontostatus, Übersicht der zentralen Aktivitätsprofile (Fahrrad, Motorrad, Wandern – per **GraphQL** geladen) und prominente Verlinkung zum interaktiven Routenplaner (SPA).
- **Benutzeranmeldung (`/login`)**: Sicheres Login über das BFF (`POST /api/v1/sessions`) mit Validierung, HttpOnly-Sitzungscookies und SameSite-Schutz.
- **Benutzerregistrierung (`/register`)**: Kontoerstellung über das BFF (`POST /api/v1/users`) mit vollständiger Eingabeprüfung und Rückmeldung.
- **Benutzerprofil (`/profile`)**: Geschützte Profilseite, die Kontodaten (`currentUser`) und Sitzungsinformationen (`currentSession`) dynamisch per **GraphQL** abruft.
- **Abmeldung (`/logout`)**: Sitzungsinvalidierung am BFF (`DELETE /api/v1/sessions/current`) und Bereinigung der lokalen Cookies.
- **Healthcheck (`/health`)**: Für Heroku- und Container-Healthchecks.
- **Verlinkung zum Routenplaner**: Verlinkt direkt auf die Single-Page Application (`https://warp-frontend-mos-25-843b86c38c7b.herokuapp.com/route-planner`).

## Technologie-Stack

- **Runtime & Framework**: Node.js 22, Express, TypeScript
- **Rendering-Engine**: EJS (Embedded JavaScript) Templates
- **API-Kommunikation**: GraphQL (für Benutzer- und Profildaten) & REST (für Authentifizierung)
- **Sicherheit**: Helmet (Security Headers & CSP), HttpOnly-Cookies
- **Testing**: Vitest & Supertest
- **Containerisierung & Hosting**: Multi-Stage Dockerfile auf Alpine Linux, Heroku Container Stack

## Lokale Entwicklung

```bash
# In das MPA-Verzeichnis wechseln
cd mpa

# Abhängigkeiten installieren
npm install

# Entwicklungsserver mit Hot-Reloading starten
npm run dev

# Tests ausführen
npm test

# Production Build erstellen
npm run build

# Gebauten Server starten
npm start
```

## Umgebungsvariablen

| Variable | Beschreibung | Standardwert |
| :--- | :--- | :--- |
| `PORT` | HTTP-Port des Servers | `8080` (bzw. `8085` in Docker Compose) |
| `BFF_URL` | Basis-URL des Backend-for-Frontend (BFF) | `http://localhost:8080` |
| `ROUTE_PLANNER_URL` | URL des interaktiven Routenplaners (SPA) | Heroku Production URL bzw. `http://localhost:5173/route-planner` |
| `NODE_ENV` | Umgebung (`development` / `production`) | `development` |

## Deployment auf Heroku

Die Anwendung ist auf Heroku unter folgender App bereitgestellt:
- **App Name**: `warp-mpa-mos-25`
- **Domain**: [https://warp-mpa-mos-25-13a73bc03543.herokuapp.com/](https://warp-mpa-mos-25-13a73bc03543.herokuapp.com/)
- **Stack**: Container
