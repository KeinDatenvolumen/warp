<!-- [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034) -->
# API Design Dokumentation – WaRP (Weather-aware Route Planning)

**Modul:** Fallstudie Software Engineering / Webprogrammierung  
**Dozent:** Prof. Dr. Christian Schalles / Michael Hartwig
**Hochschule:** DHBW Mosbach  
**Format-Vorgabe:** Vorlesung „API First“, Folie 25 (*API Design Template*)  
**Standard-Tabellenstruktur:** `Method | URI | Description | Data In | Data Out`  
**Referenzierte Spezifikationen:** `contracts/openapi/*.yaml`, `contracts/graphql/schema.graphqls`, `contracts/asyncapi/mail-events.yaml`

---

## Inhaltsverzeichnis
1. [Methodische Grundlagen und Konformität zu den Vorlesungsregeln](#1-methodische-grundlagen-und-konformität-zu-den-vorlesungsregeln)
2. [Übersicht der Architektur und Service-Ports](#2-übersicht-der-architektur-und-service-ports)
3. [Teil I: Backend-for-Frontend (BFF) – Öffentliche REST-API (Port 8080)](#3-teil-i-backend-for-frontend-bff--öffentliche-rest-api-port-8080)
   * [Endpunkt : /api/v1/system](#endpunkt--apiv1system)
   * [Endpunkt : /api/v1/csrf](#endpunkt--apiv1csrf)
   * [Endpunkt : /api/v1/users](#endpunkt--apiv1users)
   * [Endpunkt : /api/v1/sessions](#endpunkt--apiv1sessions)
   * [Endpunkt : /api/v1/email-verifications](#endpunkt--apiv1email-verifications)
   * [Endpunkt : /api/v1/password-resets](#endpunkt--apiv1password-resets)
   * [Endpunkt : /api/v1/users/me](#endpunkt--apiv1usersme)
   * [Endpunkt : /api/v1/routes](#endpunkt--apiv1routes)
4. [Teil II: Identity Service – Interne Microservice-API (Port 8081)](#4-teil-ii-identity-service--interne-microservice-api-port-8081)
   * [Endpunkt : /internal/v1/users](#endpunkt--internalv1users)
   * [Endpunkt : /internal/v1/sessions](#endpunkt--internalv1sessions)
   * [Endpunkt : /internal/v1/email-verifications](#endpunkt--internalv1email-verifications)
   * [Endpunkt : /internal/v1/password-resets](#endpunkt--internalv1password-resets)
5. [Teil III: Routing Service – Interne Microservice-API (Port 8082)](#5-teil-iii-routing-service--interne-microservice-api-port-8082)
   * [Endpunkt : /internal/v1/activity-profiles](#endpunkt--internalv1activity-profiles)
   * [Endpunkt : /internal/v1/places](#endpunkt--internalv1places)
   * [Endpunkt : /internal/v1/route-calculations](#endpunkt--internalv1route-calculations)
6. [Teil IV: Weather Service & MCP Server – Interne Microservice-API (Port 8083)](#6-teil-iv-weather-service--mcp-server--interne-microservice-api-port-8083)
   * [Endpunkt : /internal/v1/weather/forecast](#endpunkt--internalv1weatherforecast)
   * [Endpunkt : /internal/v1/weather/evaluations](#endpunkt--internalv1weatherevaluations)
   * [Endpunkt : /internal/v1/weather/optimization](#endpunkt--internalv1weatheroptimization)
   * [Endpunkt : /internal/v1/weather/ai-analysis](#endpunkt--internalv1weatherai-analysis)
   * [Endpunkt : /internal/v1/weather/mcp](#endpunkt--internalv1weathermcp)
7. [Teil V: Storage Service – Interne Microservice-API (Port 8084)](#7-teil-v-storage-service--interne-microservice-api-port-8084)
   * [Endpunkt : /internal/v1/users/{userId}/saved-routes](#endpunkt--internalv1usersuseridsaved-routes)
8. [Teil VI: Ergänzende Schnittstellenarchitektur (GraphQL & RabbitMQ)](#8-teil-vi-ergänzende-schnittstellenarchitektur-graphql--rabbitmq)

---

## 1. Methodische Grundlagen und Konformität zu den Vorlesungsregeln

Das API-Design von **WaRP** folgt strikt den in der Vorlesung *„API First“* definierten Leitlinien:

* **Resource-Naming:**  
  * Konsequenter **Plural für Ressourcen** (`/users`, `/sessions`, `/places`, `/activity-profiles`, `/saved-routes`, `/route-calculations`, `/evaluations`).
  * **Kebab-Case** für zusammengesetzte Pfadbezeichner (`/activity-profiles`, `/saved-routes`, `/route-calculations`, `/email-verifications`, `/password-resets`).
  * Sub-Ressourcen modellieren echte Domänenbeziehungen (`/users/{userId}/saved-routes/{routeId}`).
  * Vermeidung von Verben in URIs (Sub-Ressourcen wie `/password-changes` und `/email-changes` repräsentieren eigenständige Transaktionsressourcen).
* **Semantische HTTP-Methoden:**  
  * `GET` (nullipotent, sicher, cachebar) für idempotente Leseoperationen.
  * `POST` für Ressourcenerstellung, Sitzungsinitiierung und komplexe Berechnungen.
  * `PATCH` für partielle Modifikationen (z. B. Umbenennen von Routen oder Umschalten des Favoritenstatus).
  * `DELETE` für das rückstandslose Entfernen von Ressourcen und Sitzungen.
* **Unterstützung für Frontend-Entwickler:**  
  * Paginierung (`page`, `size`), Filterung (`favorite=true`) und deterministische Sortierung (Favoriten vor Normalrouten, neueste zuerst) bei Listenabfragen (`/saved-routes`).
  * Parametrisierte Suche mit Mindestlänge und Begrenzung (`/places?query=...&limit=5`).
  * Entkopplung von der Datenbankstruktur: Die API exponiert aufgabengerechte DTOs statt interner Tabellenschemata.
* **Gelebte Asynchronität:**  
  * Langlaufende Berechnungen (OpenRouteService) blockieren den Client nicht: `POST /internal/v1/route-calculations` antwortet unmittelbar mit `202 Accepted` und einem `Location`-Header zur nicht-blockierenden Statusabfrage via Polling.
  * Zeitkritische Nebenläufigkeiten (E-Mail-Versand) werden transaktionsentkoppelt über Message-Queues (RabbitMQ) verarbeitet.
* **Strikte Zustandslosigkeit & Sicherheit:**  
  * Der BFF hält keine serverseitige In-Memory-Sitzung. Die Identifikation erfolgt über kryptographisch signierte Session-Token im `HttpOnly`-Cookie.
  * Einheitliche Fehlerformate nach **RFC 7807 Problem Details** (`application/problem+json`).

---

## 2. Übersicht der Architektur und Service-Ports

| Dienst / Schicht | Port | Protokoll / Format | Aufgabenbereich |
| :--- | :---: | :--- | :--- |
| **BFF Service** | `8080` | REST (`/api/v1/*`) & GraphQL (`/graphql`) | Öffentliches API-Gateway, CSRF-Schutz, Cookie-Management, Orchestrierung |
| **Identity Service** | `8081` | Internes REST (`/internal/v1/*`) | Kontoverwaltung, Session-Lookup mit Caffeine-Cache, RabbitMQ-Mail-Events |
| **Routing Service** | `8082` | Internes REST (`/internal/v1/*`) | Aktivitätsprofile, MapTiler-Geocoding mit Cache, asynchrones Routing (ORS) |
| **Weather Service** | `8083` | Internes REST & JSON-RPC 2.0 (MCP) | Open-Meteo-Wetter, 101-Stufen-Scoring, Abfahrtszeit-Optimierung, Gemini-KI |
| **Storage Service** | `8084` | Internes REST (`/internal/v1/*`) | Persistenz gespeicherter Routen, Snapshots, Paginierung und Favoriten |

> **Hinweis zur Absicherung interner Endpunkte:**  
> Sämtliche internen Microservice-Endpunkte (`/internal/v1/*`) erfordern zwingend den Header `X-WaRP-Service-Token`.

---

## 3. Teil I: Backend-for-Frontend (BFF) – Öffentliche REST-API (Port 8080)

### Endpunkt : /api/v1/system

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/system/health` | Aggregierter System- und Readiness-Check aller fünf WaRP-Dienste inklusive individueller Latenzmessung. | *Keine Parameter* | **200 OK** (Alle Dienste betriebsbereit):<br>`{ "status": "UP", "checkedAt": "2026-11-08T12:00:00Z", "healthyServices": 5, "totalServices": 5, "services": [ { "service": "bff-service", "status": "UP", "reachable": true, "responseTimeMs": 1 }, ... ] }`<br><br>**503 Service Unavailable** (Mind. ein nachgelagerter Dienst ausgefallen):<br>`{ "status": "DEGRADED", "checkedAt": "...", "healthyServices": 4, "totalServices": 5, "services": [...] }` |

---

### Endpunkt : /api/v1/csrf

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/csrf` | Ruft ein frisches CSRF-Token nach dem Double-Submit-Cookie-Pattern ab. Zwingend erforderlich für alle zustandsverändernden REST- und GraphQL-Aufrufe. | *Keine Parameter* | **200 OK**:<br>`{ "token": "a9f8b2c1-...", "headerName": "X-CSRF-TOKEN", "parameterName": "_csrf" }` |

---

### Endpunkt : /api/v1/users

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/users` | Registriert ein neues Benutzerkonto mit Name, E-Mail und Passwort (min. 12 Zeichen). Stößt nach Datenbank-Commit asynchron eine Bestätigungs-E-Mail über RabbitMQ an. | **Header:**<br>`X-CSRF-TOKEN: string`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"name": "string",`<br>&nbsp;&nbsp;`"email": "string (email)",`<br>&nbsp;&nbsp;`"password": "string (min 12 Zeichen)"`<br>`}` | **201 Created**:<br>`{ "id": "usr_...", "name": "Max Mustermann", "email": "max@example.com", "emailVerified": false, "createdAt": "2026-11-08T12:00:00Z", "message": "Benutzerkonto erstellt; Bestätigungs-E-Mail wird zugestellt" }`<br><br>**409 Conflict**:<br>`{ "status": 409, "code": "EMAIL_ALREADY_EXISTS", "detail": "E-Mail-Adresse existiert bereits" }`<br><br>**422 Unprocessable Content**:<br>`{ "status": 422, "code": "VALIDATION_FAILED", "fieldErrors": [ { "field": "password", "message": "Mindestens 12 Zeichen erforderlich" } ] }` |

---

### Endpunkt : /api/v1/sessions

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/sessions` | Authentifiziert den Benutzer gegen den Identity-Service. Prüft Passwörter timing-resistent gegen Dummy-Hashes (`dummyPasswordHash`), setzt ein `HttpOnly`-Session-Cookie (`PT12H`) und schützt über `LoginRateLimiter` (max. 5 Versuche / 60s pro `email\|ip`) vor Brute-Force. | **Header:**<br>`X-CSRF-TOKEN: string`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"email": "string (email)",`<br>&nbsp;&nbsp;`"password": "string"`<br>`}` | **200 OK**:<br>**Header:**<br>`Set-Cookie: session=<token>; Path=/; Secure; HttpOnly; SameSite=None`<br>**Body:** `{ "userId": "usr_...", "expiresAt": "2026-11-09T00:00:00Z" }`<br><br>**401 Unauthorized**:<br>`{ "status": 401, "code": "INVALID_CREDENTIALS", "detail": "Ungültige E-Mail-Adresse oder Passwort" }`<br><br>**429 Too Many Requests**:<br>**Header:** `Retry-After: 60`<br>`{ "status": 429, "code": "TOO_MANY_LOGIN_ATTEMPTS", "detail": "Zu viele Fehlversuche. Bitte 60 Sekunden warten." }` |
| `DELETE` | `/api/v1/sessions/current` | Beendet die aktuelle Benutzersitzung. Invalidiert das Sitzungs-Token in der Datenbank, entfernt den Eintrag aus dem In-Memory-Cache des Identity-Services und löscht das Browser-Cookie. | **Header:**<br>`X-CSRF-TOKEN: string`<br>`Cookie: session=<token>` | **204 No Content**:<br>**Header:**<br>`Set-Cookie: session=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=None` |

---

### Endpunkt : /api/v1/email-verifications

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/email-verifications/confirm` | Bestätigt die E-Mail-Adresse anhand eines Einmal-Verifikations-Tokens. Schaltet Routenberechnung und Speichern frei. | **Header:**<br>`X-CSRF-TOKEN: string`<br><br>**Body (`application/json`):**<br>`{ "token": "string (UUID)" }` | **204 No Content**: E-Mail-Adresse erfolgreich bestätigt.<br><br>**400 Bad Request**:<br>`{ "status": 400, "code": "INVALID_EMAIL_VERIFICATION_TOKEN", "detail": "Token ist abgelaufen oder ungültig" }` |
| `POST` | `/api/v1/email-verifications/resend` | Fordert den erneuten Versand einer Bestätigungs-E-Mail an. Verhindert User-Enumeration durch identische Antworten. | **Header:**<br>`X-CSRF-TOKEN: string`<br><br>**Body (`application/json`):**<br>`{ "email": "string (email)" }` | **204 No Content**: Anfrage entgegengenommen. |

---

### Endpunkt : /api/v1/password-resets

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/password-resets/send` | Erzeugt ein zeitlich befristetes Passwort-Reset-Token und stellt die Benachrichtigung asynchron über RabbitMQ zu. | **Header:**<br>`X-CSRF-TOKEN: string`<br><br>**Body (`application/json`):**<br>`{ "email": "string (email)" }` | **204 No Content**: Anfrage entgegengenommen (verrät nicht, ob das Konto existiert). |
| `POST` | `/api/v1/password-resets/confirm` | Setzt das Kontopasswort unter Verwendung eines gültigen Reset-Tokens auf einen neuen Wert zurück. | **Header:**<br>`X-CSRF-TOKEN: string`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"token": "string",`<br>&nbsp;&nbsp;`"newPassword": "string (min 12 Zeichen)"`<br>`}` | **204 No Content**: Passwort erfolgreich geändert.<br><br>**400 Bad Request**:<br>`{ "status": 400, "code": "INVALID_PASSWORD_RESET_TOKEN" }`<br><br>**422 Unprocessable Content**:<br>`{ "status": 422, "code": "PASSWORD_REQUIREMENTS_NOT_MET" }` |

---

### Endpunkt : /api/v1/users/me

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/users/me/password-changes` | Ändert das Passwort des aktuell angemeldeten Benutzers nach Prüfung des bisherigen Kennworts. | **Header:**<br>`X-CSRF-TOKEN: string`<br>`Cookie: session=<token>`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"currentPassword": "string",`<br>&nbsp;&nbsp;`"newPassword": "string (min 12 Zeichen)"`<br>`}` | **204 No Content**: Passwort geändert.<br><br>**401 Unauthorized**:<br>`{ "code": "NOT_AUTHENTICATED" }`<br><br>**403 Forbidden**:<br>`{ "code": "INVALID_PASSWORD" }`<br><br>**422 Unprocessable Content**:<br>`{ "code": "PASSWORD_REQUIREMENTS_NOT_MET" }` |
| `POST` | `/api/v1/users/me/email-changes` | Beantragt eine E-Mail-Änderung (`pending_email`) nach Passwortprüfung und versendet einen Bestätigungslink an die neue Adresse. Die aktive E-Mail wechselt erst nach Klick auf den Verifikationslink. | **Header:**<br>`X-CSRF-TOKEN: string`<br>`Cookie: session=<token>`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"newEmail": "string (email)",`<br>&nbsp;&nbsp;`"password": "string"`<br>`}` | **204 No Content**: E-Mail-Änderung vorgemerkt und Bestätigungs-E-Mail versendet.<br><br>**401 Unauthorized**:<br>`{ "code": "NOT_AUTHENTICATED" }`<br><br>**403 Forbidden**:<br>`{ "code": "INVALID_PASSWORD" }`<br><br>**409 Conflict**:<br>`{ "code": "EMAIL_ALREADY_EXISTS" }`<br><br>**422 Unprocessable Content**:<br>`{ "code": "INVALID_ACCOUNT_CHANGE_REQUEST" }` |

---

### Endpunkt : /api/v1/routes

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/routes/weather-optimization` | Simuliert Abfahrtszeitfenster entlang der Streckengeometrie, wendet Gauß-Glättung (`0.20 / 0.60 / 0.20`) an und ermittelt das optimale Abfahrtszeitfenster mit dem höchsten Wetterscore. | **Header:**<br>`X-CSRF-TOKEN: string`<br>optional `Cookie: session=<token>`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"routeCalculationId": "calc_...",`<br>&nbsp;&nbsp;`"activityProfile": "BICYCLE",`<br>&nbsp;&nbsp;`"departureTime": "2026-11-08T10:00:00Z",`<br>&nbsp;&nbsp;`"durationSeconds": 7200,`<br>&nbsp;&nbsp;`"distanceMeters": 45000,`<br>&nbsp;&nbsp;`"coordinates": [[9.18, 48.77], ...],`<br>&nbsp;&nbsp;`"windowStart": "2026-11-08T08:00:00Z",`<br>&nbsp;&nbsp;`"windowEnd": "2026-11-08T14:00:00Z"`<br>`}` | **200 OK**:<br>`{`<br>&nbsp;&nbsp;`"recommendedSlotIndex": 2,`<br>&nbsp;&nbsp;`"optimalWindowSummary": { "bestDeparture": "11:00", "bestDepartureIso": "...", "bestScore": 92, "stableWindow": "10:00 – 12:00", "advantageVsWorst": "+24 Punkte ggü. 08:00 Uhr", "worstDeparture": "08:00", "worstScore": 68 },`<br>&nbsp;&nbsp;`"departureSlots": [`<br>&nbsp;&nbsp;&nbsp;&nbsp;`{ "departureIso": "...", "departureTime": "11:00", "arrivalIso": "...", "arrivalTime": "13:00", "routeScore": 92, "robustScore": 90.4, "hasThunderCap": false, "reachesNightfall": false, "segmentScores": [92, 91], "segmentReasons": ["Optimale Bedingungen"], "summaryDeltaText": "+24 Pkt. ggü. 08:00 Uhr" }` <br>&nbsp;&nbsp;`]`<br>`}` |
| `POST` | `/api/v1/routes/weather-ai-analysis` | Führt eine deterministische Tool-basierte KI-Wetteranalyse (Google Gemini + MCP-Server mit 10s Timeout & Physik-Engine-Fallback) für eine Route durch. Erfordert Login und verifizierte E-Mail. | **Header:**<br>`X-CSRF-TOKEN: string`<br>`Cookie: session=<token>`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"routeCalculationId": "calc_...",`<br>&nbsp;&nbsp;`"startLabel": "Stuttgart",`<br>&nbsp;&nbsp;`"destinationLabel": "Tübingen",`<br>&nbsp;&nbsp;`"activityProfile": "BICYCLE",`<br>&nbsp;&nbsp;`"departureTime": "2026-11-08T10:00:00Z",`<br>&nbsp;&nbsp;`"durationSeconds": 7200,`<br>&nbsp;&nbsp;`"distanceMeters": 45000,`<br>&nbsp;&nbsp;`"coordinates": [[9.18, 48.77], ...]`<br>`}` | **200 OK**:<br>`{`<br>&nbsp;&nbsp;`"routeCalculationId": "calc_...",`<br>&nbsp;&nbsp;`"activityProfileCode": "BICYCLE",`<br>&nbsp;&nbsp;`"activityProfileDisplayName": "Fahrrad",`<br>&nbsp;&nbsp;`"departureTime": "2026-11-08T10:00:00Z",`<br>&nbsp;&nbsp;`"overallScore": 85,`<br>&nbsp;&nbsp;`"thunderstormCapApplied": false,`<br>&nbsp;&nbsp;`"riskLevel": "LOW",`<br>&nbsp;&nbsp;`"headline": "Gute Bedingungen mit leichtem Seitenwind",`<br>&nbsp;&nbsp;`"overallSummary": "...",`<br>&nbsp;&nbsp;`"activityProfileAdvice": "...",`<br>&nbsp;&nbsp;`"hiddenParameterInsights": [...],`<br>&nbsp;&nbsp;`"criticalSections": [...],`<br>&nbsp;&nbsp;`"recommendations": ["Windweste empfohlen"],`<br>&nbsp;&nbsp;`"mcpToolsUsed": ["get_route_weather_telemetry", "detect_hidden_meteorological_anomalies", "get_activity_profile_vulnerability_matrix"],`<br>&nbsp;&nbsp;`"modelName": "gemini-3.5-flash (MCP)",`<br>&nbsp;&nbsp;`"generatedAt": "2026-11-08T12:00:00Z"`<br>`}`<br><br>**401 Unauthorized**:<br>`{ "code": "SESSION_INVALID" }`<br><br>**403 Forbidden**:<br>`{ "code": "EMAIL_NOT_VERIFIED" }` |

---

## 4. Teil II: Identity Service – Interne Microservice-API (Port 8081)

### Endpunkt : /internal/v1/users

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/internal/v1/users` | Erstellt ein Benutzerkonto mit BCrypt-Hash und reiht nach DB-Commit ein E-Mail-Event in RabbitMQ (`warp.mail.send`) ein. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"name": "string",`<br>&nbsp;&nbsp;`"email": "string (email)",`<br>&nbsp;&nbsp;`"password": "string (min 12)"`<br>`}` | **201 Created**:<br>`{ "id": "usr_...", "name": "...", "email": "...", "emailVerified": false, "createdAt": "..." }`<br><br>**409 Conflict**:<br>`{ "type": "about:blank", "title": "Conflict", "status": 409, "code": "EMAIL_ALREADY_EXISTS" }` |
| `GET` | `/internal/v1/users/{userId}` | Ruft die Stammdaten eines Benutzers anhand der ID ab. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Pfad:**<br>`userId: string` | **200 OK**:<br>`{ "id": "usr_...", "name": "...", "email": "...", "emailVerified": true, "createdAt": "..." }`<br><br>**404 Not Found**:<br>`{ "status": 404, "code": "USER_NOT_FOUND" }` |
| `POST` | `/internal/v1/users/{userId}/password-changes` | Führt die interne Passwortänderung mit Hash-Verifikation durch. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Pfad:** `userId: string`<br><br>**Body (`application/json`):**<br>`{ "currentPassword": "...", "newPassword": "..." }` | **204 No Content**: Passwort geändert.<br><br>**403 Forbidden**:<br>`{ "status": 403, "code": "INVALID_PASSWORD" }` |
| `POST` | `/internal/v1/users/{userId}/email-changes` | Ändert die E-Mail-Adresse und setzt den Verifikationsstatus zurück. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Pfad:** `userId: string`<br><br>**Body (`application/json`):**<br>`{ "newEmail": "...", "password": "..." }` | **204 No Content**: E-Mail geändert.<br><br>**409 Conflict**:<br>`{ "status": 409, "code": "EMAIL_ALREADY_EXISTS" }` |

---

### Endpunkt : /internal/v1/sessions

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/internal/v1/sessions` | Erzeugt ein kryptographisches Session-Token und persistiert dessen SHA-256-Hash in PostgreSQL. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Body (`application/json`):**<br>`{ "email": "...", "password": "..." }` | **201 Created**:<br>`{ "sessionToken": "raw_token_...", "userId": "usr_...", "createdAt": "...", "expiresAt": "..." }`<br><br>**401 Unauthorized**:<br>`{ "status": 401, "code": "INVALID_CREDENTIALS" }` |
| `POST` | `/internal/v1/sessions/validation` | Validiert ein Session-Token über den SHA-256-Hash. Nutzt den lokalen **Caffeine In-Memory-Cache** (TTL 5 min, `sync=true`) mit unveränderlichen `CachedSession`-Records. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Body (`application/json`):**<br>`{ "sessionToken": "raw_token_..." }` | **200 OK**:<br>`{ "id": "ses_...", "userId": "usr_...", "createdAt": "...", "expiresAt": "...", "emailVerified": true }`<br><br>**401 Unauthorized**:<br>`{ "status": 401, "code": "SESSION_EXPIRED" }`<br><br>**404 Not Found**:<br>`{ "status": 404, "code": "SESSION_NOT_FOUND" }` |
| `POST` | `/internal/v1/sessions/invalidation` | Invalidiert die Sitzung in der DB und entfernt den Eintrag sofort via `@CacheEvict` aus Caffeine. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Body (`application/json`):**<br>`{ "sessionToken": "raw_token_..." }` | **204 No Content**: Sitzung invalidiert und Cache bereinigt. |

---

### Endpunkt : /internal/v1/email-verifications

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/internal/v1/email-verifications/confirm` | Validiert den Verifikations-Token-Hash und setzt `email_verified = true`. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Body (`application/json`):**<br>`{ "token": "string" }` | **204 No Content**: Verifikation erfolgreich.<br><br>**400 Bad Request**:<br>`{ "code": "INVALID_EMAIL_VERIFICATION_TOKEN" }` |
| `POST` | `/internal/v1/email-verifications/resend` | Erzeugt ein neues Verifikations-Token und sendet eine AMQP-Nachricht an RabbitMQ. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Body (`application/json`):**<br>`{ "email": "string" }` | **204 No Content**: Event eingereiht. |

---

### Endpunkt : /internal/v1/password-resets

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/internal/v1/password-resets/send` | Erzeugt ein Reset-Token und stellt die E-Mail via RabbitMQ zu. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Body (`application/json`):**<br>`{ "email": "string" }` | **204 No Content**: Anfrage verarbeitet. |
| `POST` | `/internal/v1/password-resets/confirm` | Setzt das Passwort zurück und invalidiert das Reset-Token. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Body (`application/json`):**<br>`{ "token": "...", "newPassword": "..." }` | **204 No Content**: Passwort geändert.<br><br>**400 Bad Request**:<br>`{ "code": "INVALID_PASSWORD_RESET_TOKEN" }` |

---

## 5. Teil III: Routing Service – Interne Microservice-API (Port 8082)

### Endpunkt : /internal/v1/activity-profiles

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/internal/v1/activity-profiles` | Liefert alle zentral konfigurierten Aktivitätsprofile (`BICYCLE`, `MOTORCYCLE`, `HIKING`) samt Durchschnittsgeschwindigkeiten, ORS-Routingprofilen und Wetterabfrage-Intervallen. | **Header:**<br>`X-WaRP-Service-Token: string` | **200 OK**:<br>`[`<br>&nbsp;&nbsp;`{`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"code": "BICYCLE",`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"displayName": "Fahrrad",`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"averageSpeedKmh": 18.0,`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"routingProfile": "CYCLING_REGULAR",`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"weatherQueryIntervalKm": 5,`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"wayPreferences": { "preferCycleways": true, "avoidHighways": true }`<br>&nbsp;&nbsp;`}, ...`<br>`]` |
| `GET` | `/internal/v1/activity-profiles/{code}` | Ruft ein einzelnes Profil anhand des Enums ab. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Pfad:**<br>`code: BICYCLE \| MOTORCYCLE \| HIKING` | **200 OK**: Einzelnes Aktivitätsprofil-Objekt.<br><br>**400 Bad Request**: Ungültiger Profilcode. |

---

### Endpunkt : /internal/v1/places

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/internal/v1/places` | Sucht Orte und Koordinaten über die MapTiler Geocoding API. Eingaben werden getrimmt und kleingeschrieben; Ergebnisse werden für 24h im lokalen **Caffeine-Cache** (`geocoding-results`, max. 5.000 Einträge, `sync=true`) gepuffert. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Query-Parameter:**<br>`query: string (min 3, max 200)`<br>`limit: integer (1..10, default 5)` | **200 OK**:<br>`{`<br>&nbsp;&nbsp;`"places": [`<br>&nbsp;&nbsp;&nbsp;&nbsp;`{ "placeId": "map_123", "displayName": "Stuttgart, Baden-Württemberg", "latitude": 48.7758, "longitude": 9.1829 }, ...`<br>&nbsp;&nbsp;`]`<br>`}`<br><br>**503 Service Unavailable**:<br>`{ "code": "GEOCODING_SERVICE_UNAVAILABLE" }` |

---

### Endpunkt : /internal/v1/route-calculations

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/internal/v1/route-calculations` | **Asynchrone Routenberechnung (Folie 22):** Persistiert den Berechnungsauftrag mit Status `PENDING`, startet die OpenRouteService-Berechnung im Hintergrund und antwortet sofort nicht-blockierend mit `202 Accepted` und `Location`-Header. Unterstützt `Idempotency-Key`. | **Header:**<br>`X-WaRP-Service-Token: string`<br>`Idempotency-Key: string`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"requestedByUserId": "usr_...",`<br>&nbsp;&nbsp;`"start": { "label": "Start", "latitude": 48.77, "longitude": 9.18 },`<br>&nbsp;&nbsp;`"destination": { "label": "Ziel", "latitude": 48.52, "longitude": 9.05 },`<br>&nbsp;&nbsp;`"departureTime": "2026-11-08T10:00:00Z",`<br>&nbsp;&nbsp;`"activityProfile": { "code": "BICYCLE", "averageSpeedKmh": 18, "routingProfile": "CYCLING_REGULAR", "weatherQueryIntervalKm": 5 }`<br>`}` | **202 Accepted**:<br>**Header:**<br>`Location: /internal/v1/route-calculations/calc_...`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"id": "calc_123456",`<br>&nbsp;&nbsp;`"status": "PENDING",`<br>&nbsp;&nbsp;`"createdAt": "2026-11-08T10:00:00Z"`<br>`}`<br><br>**400 Bad Request**:<br>`{ "code": "INVALID_ROUTE_CALCULATION_REQUEST" }` |
| `GET` | `/internal/v1/route-calculations/{calculationId}` | Polling-Endpunkt für den Berechnungsstatus. Liefert bei Status `COMPLETED` die vollständige GeoJSON-Liniengeometrie und segmentierte Distanz-/Dauerdaten. | **Header:**<br>`X-WaRP-Service-Token: string`<br>`X-User-Id: string (optional)`<br><br>**Pfad:**<br>`calculationId: string` | **200 OK**:<br>`{`<br>&nbsp;&nbsp;`"id": "calc_123456",`<br>&nbsp;&nbsp;`"status": "COMPLETED",`<br>&nbsp;&nbsp;`"departureTime": "2026-11-08T10:00:00Z",`<br>&nbsp;&nbsp;`"arrivalTime": "2026-11-08T12:30:00Z",`<br>&nbsp;&nbsp;`"distanceMeters": 45200,`<br>&nbsp;&nbsp;`"durationSeconds": 9000,`<br>&nbsp;&nbsp;`"geometry": { "type": "LineString", "coordinates": [[9.18, 48.77], ...] },`<br>&nbsp;&nbsp;`"createdAt": "..."`<br>`}`<br><br>**404 Not Found**:<br>`{ "code": "ROUTE_CALCULATION_NOT_FOUND" }` |

---

## 6. Teil IV: Weather Service & MCP Server – Interne Microservice-API (Port 8083)

### Endpunkt : /internal/v1/weather/forecast

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/internal/v1/weather/forecast` | Ruft stündliche Wettervorhersagedaten von Open-Meteo für spezifische Koordinaten und Zeitfenster ab. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Query-Parameter:**<br>`locationId: string`<br>`latitude: number (-90..90)`<br>`longitude: number (-180..180)`<br>`start: date-time`<br>`end: date-time` | **200 OK**:<br>`{ "locationId": "...", "latitude": 48.77, "longitude": 9.18, "hourly": { "time": ["2026-11-08T10:00"], "temperature2m": [14.2], "precipitationProbability": [10], ... } }` |

---

### Endpunkt : /internal/v1/weather/evaluations

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/internal/v1/weather/evaluations` | Berechnet die raum-zeitliche Wetterkorrelation entlang einer Route: Wegpunkt-Interpolation, stündlicher Open-Meteo-Abruf, 101-stufiges Perceptual Scoring (0–100) und Gewitterkappung (*Thunderstorm Cap* auf max. 25 Punkte). | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"requestedByUserId": "usr_...",`<br>&nbsp;&nbsp;`"route": {`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"routeCalculationId": "calc_...",`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"departureTime": "2026-11-08T10:00:00Z",`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"durationSeconds": 7200,`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"distanceMeters": 45000,`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"coordinates": [[9.18, 48.77], ...],`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"activityProfile": { "code": "BICYCLE", "averageSpeedKmh": 18, "weatherQueryIntervalKm": 5 }`<br>&nbsp;&nbsp;`}`<br>`}` | **200 OK**:<br>`{`<br>&nbsp;&nbsp;`"evaluationId": "weval_...",`<br>&nbsp;&nbsp;`"routeCalculationId": "calc_...",`<br>&nbsp;&nbsp;`"status": "COMPLETED",`<br>&nbsp;&nbsp;`"overallScore": 74,`<br>&nbsp;&nbsp;`"thunderstormCapApplied": false,`<br>&nbsp;&nbsp;`"segments": [`<br>&nbsp;&nbsp;&nbsp;&nbsp;`{ "segmentIndex": 0, "distanceFromStartMeters": 5000, "expectedArrivalTime": "...", "score": 85, "color": "#22c55e", "limitingFactor": "WIND" }, ...`<br>&nbsp;&nbsp;`]`<br>`}` |
| `GET` | `/internal/v1/weather/evaluations/{evaluationId}` | Ruft eine persistierte Wetterbewertung anhand ihrer ID ab. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Pfad:** `evaluationId: string` | **200 OK**: Wetterbewertungs-Objekt.<br><br>**404 Not Found**: Keine Bewertung gefunden. |
| `GET` | `/internal/v1/weather/evaluations/by-route/{routeCalculationId}` | Ruft die Wetterbewertung zu einer Routenberechnungs-ID ab. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Pfad:** `routeCalculationId: string` | **200 OK**: Wetterbewertungs-Objekt.<br><br>**404 Not Found**: Keine Bewertung zur Route vorhanden. |

---

### Endpunkt : /internal/v1/weather/optimization

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/internal/v1/weather/optimization` | Simuliert alternative Abfahrtszeitfenster in 15-/30-Minuten-Schritten und ermittelt den optimalen Startzeitpunkt. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Body (`application/json`):**<br>`{ "requestedByUserId": "usr_...", "route": { ... }, "windowStart": "2026-11-08T08:00:00Z", "windowEnd": "2026-11-08T14:00:00Z" }` | **200 OK**:<br>`{ "routeCalculationId": "calc_...", "currentDepartureTime": "...", "currentScore": 68, "optimalDepartureTime": "2026-11-08T11:00:00Z", "optimalScore": 92, "scoreImprovement": 24, "slots": [...] }` |

---

### Endpunkt : /internal/v1/weather/ai-analysis

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/internal/v1/weather/ai-analysis` | Führt die KI-gestützte Routenanalyse mit Google Gemini und den internen MCP-Tools durch. | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Body (`application/json`):**<br>`{ "requestedByUserId": "usr_...", "route": { ... }, "startLabel": "Stuttgart", "destinationLabel": "Tübingen", "userPrompt": "Gibt es Unwetterwarnungen?" }` | **200 OK**:<br>`{ "routeCalculationId": "calc_...", "activityProfile": "BICYCLE", "overallScore": 85, "verdictLevel": "GOOD", "headline": "...", "summary": "...", "tacticalAdvice": [...], "gearRecommendations": [...], "mcpToolsInvoked": ["analyze_wind_hazard"], "aiPowered": true, "modelUsed": "gemini-2.0-flash", "generatedAt": "..." }` |

---

### Endpunkt : /internal/v1/weather/mcp

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/internal/v1/weather/mcp` | **Model Context Protocol (MCP) JSON-RPC 2.0 Endpunkt (Protokollversion 2024-11-05):** Ermöglicht LLMs die deterministische Abfrage von Wetter-Tools (`initialize`, `tools/list`, `tools/call`). | **Header:**<br>`X-WaRP-Service-Token: string`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"jsonrpc": "2.0",`<br>&nbsp;&nbsp;`"id": 1,`<br>&nbsp;&nbsp;`"method": "tools/call",`<br>&nbsp;&nbsp;`"params": {`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"name": "analyze_wind_hazard",`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"arguments": { "windSpeedKmh": 45, "windGustsKmh": 68, "activityProfile": "BICYCLE" }`<br>&nbsp;&nbsp;`}`<br>`}` | **200 OK**:<br>`{`<br>&nbsp;&nbsp;`"jsonrpc": "2.0",`<br>&nbsp;&nbsp;`"id": 1,`<br>&nbsp;&nbsp;`"result": {`<br>&nbsp;&nbsp;&nbsp;&nbsp;`"content": [`<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`{ "type": "text", "text": "{\"hazardLevel\":\"HIGH\",\"criticalWind\":true,\"reason\":\"Starke Seitenwindböen über 60 km/h gefährden Radfahrer.\"}" }`<br>&nbsp;&nbsp;&nbsp;&nbsp;`]`<br>&nbsp;&nbsp;`}`<br>`}` |

---

## 7. Teil V: Storage Service – Interne Microservice-API (Port 8084)

### Endpunkt : /internal/v1/users/{userId}/saved-routes

| Method | URI | Description | Data In | Data Out |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/internal/v1/users/{userId}/saved-routes` | Speichert einen vollständigen Snapshot aus Routengeometrie, Aktivitätsprofil und Wetterbewertung benutzergebunden ab. Unterstützt `Idempotency-Key` gegen Doppelungen bei Netzwerk-Timeouts. | **Header:**<br>`X-WaRP-Service-Token: string`<br>`X-User-Id: string`<br>`Idempotency-Key: string (optional)`<br><br>**Pfad:** `userId: string`<br><br>**Body (`application/json`):**<br>`{`<br>&nbsp;&nbsp;`"name": "string (1..100)",`<br>&nbsp;&nbsp;`"favorite": false,`<br>&nbsp;&nbsp;`"routeSnapshot": { "routeCalculationId": "calc_...", "start": { "label": "Start", "latitude": 48.77, "longitude": 9.18 }, "destination": { "label": "Ziel", "latitude": 48.52, "longitude": 9.05 }, "activityProfileCode": "BICYCLE", "departureTime": "...", "arrivalTime": "...", "distanceMeters": 45000, "durationSeconds": 7200, "geometry": { "type": "LineString", "coordinates": [...] } },`<br>&nbsp;&nbsp;`"weatherSnapshot": { "weatherEvaluationId": "weval_...", "overallScore": 82, "scoreVersion": "1.0", "thunderstormCapApplied": false, "segments": [...] }`<br>`}` | **201 Created** (Erfolgreich neu gespeichert):<br>`{ "id": "sroute_123", "name": "Hausrunde", "favorite": false, "routeSnapshot": { ... }, "weatherSnapshot": { ... }, "createdAt": "...", "updatedAt": "..." }`<br><br>**200 OK** (Idempotente Wiederholung liefert existierende Route):<br>`{ "id": "sroute_123", ... }`<br><br>**401 / 403 / 422**:<br>`application/problem+json` |
| `GET` | `/internal/v1/users/{userId}/saved-routes` | **Paginierte und sortierte Routenübersicht (Folie 19):** Liefert kompakte Zusammenfassungen der gespeicherten Routen. Favoriten stehen immer an oberster Stelle; innerhalb beider Gruppen gilt `createdAt DESC`. | **Header:**<br>`X-WaRP-Service-Token: string`<br>`X-User-Id: string`<br><br>**Pfad:** `userId: string`<br><br>**Query-Parameter:**<br>`favorite: boolean (optional)`<br>`page: integer (>= 0, default 0)`<br>`size: integer (1..50, default 20)` | **200 OK**:<br>`{`<br>&nbsp;&nbsp;`"items": [`<br>&nbsp;&nbsp;&nbsp;&nbsp;`{ "id": "sroute_123", "name": "Hausrunde", "start": { "label": "Start", "latitude": 48.77, "longitude": 9.18 }, "destination": { "label": "Ziel", "latitude": 48.52, "longitude": 9.05 }, "activityProfileCode": "BICYCLE", "weatherScore": 82, "favorite": true, "createdAt": "..." }, ...`<br>&nbsp;&nbsp;`],`<br>&nbsp;&nbsp;`"page": 0,`<br>&nbsp;&nbsp;`"size": 20,`<br>&nbsp;&nbsp;`"totalElements": 1,`<br>&nbsp;&nbsp;`"totalPages": 1`<br>`}` |
| `GET` | `/internal/v1/users/{userId}/saved-routes/{routeId}` | Ruft eine eigene gespeicherte Route inklusive aller Segmente, Wettermetriken und GeoJSON-Geometrie ab. | **Header:**<br>`X-WaRP-Service-Token: string`<br>`X-User-Id: string`<br><br>**Pfad:**<br>`userId: string`<br>`routeId: string` | **200 OK**:<br>`{ "id": "sroute_123", "name": "Hausrunde", "favorite": false, "routeSnapshot": { ... }, "weatherSnapshot": { ... }, "createdAt": "...", "updatedAt": "..." }`<br><br>**404 Not Found**:<br>`{ "status": 404, "code": "SAVED_ROUTE_NOT_FOUND" }` |
| `PATCH` | `/internal/v1/users/{userId}/saved-routes/{routeId}` | Ändert partiell den Anzeigenamen und/oder den Favoritenstatus einer Route. | **Header:**<br>`X-WaRP-Service-Token: string`<br>`X-User-Id: string`<br><br>**Pfad:**<br>`userId: string`<br>`routeId: string`<br><br>**Body (`application/json`):**<br>`{ "name": "Neuer Name (optional)", "favorite": true }` | **200 OK**:<br>`{ "id": "sroute_123", "name": "Neuer Name", "favorite": true, ... }` |
| `DELETE` | `/internal/v1/users/{userId}/saved-routes/{routeId}` | Löscht eine gespeicherte Route des Benutzers endgültig. | **Header:**<br>`X-WaRP-Service-Token: string`<br>`X-User-Id: string`<br><br>**Pfad:**<br>`userId: string`<br>`routeId: string` | **204 No Content**: Route erfolgreich gelöscht.<br><br>**404 Not Found**: Route existiert nicht oder gehört anderem Benutzer. |

---

## 8. Teil VI: Ergänzende Schnittstellenarchitektur (GraphQL & RabbitMQ)

### 8.1 GraphQL-Domänen-Mapping am BFF (`POST /graphql`)

Neben den oben spezifizierten REST-Endpunkten stellt das BFF eine komplementäre GraphQL-Schnittstelle gemäß `contracts/graphql/schema.graphqls` bereit. Dies eliminiert *Overfetching* in der MPA und *Underfetching* in der SPA:

| GraphQL Operation | Typ | Orchestrierte Microservices | Verwendungszweck |
| :--- | :---: | :--- | :--- |
| `currentUser` | Query | `identity-service` (`GET /internal/v1/users/{id}`) | Lädt Kontodaten des angemeldeten Benutzers für Profil und Navbar. |
| `currentSession` | Query | `identity-service` (`POST /internal/v1/sessions/validation`) | Prüft clientseitig Gültigkeit und Verifikationsstatus der Sitzung. |
| `activityProfiles` | Query | `routing-service` (`GET /internal/v1/activity-profiles`) | Lädt konfigurierte Aktivitäten (Rad, Motorrad, Wandern) im Routenplaner. |
| `searchPlaces(query, limit)` | Query | `routing-service` (`GET /internal/v1/places`) | Autocomplete-Ortssuche im Start- und Zielfeld. |
| `startRouteCalculation(...)` | Mutation | `routing-service` (`POST /internal/v1/route-calculations`) | Startet asynchrones Routing; liefert `calculationId` für Polling. |
| `routeCalculation(id)` | Query | `routing-service` & `weather-service` | **Single-Fetch-Aggregation:** Führt Geometrie aus Routing und Segmente aus Wetterbewertung in einer Abfrage zusammen. |
| `savedRoutes(favorite, page, size)`| Query | `storage-service` (`GET /internal/v1/users/{userId}/saved-routes`) | Paginierte Liste gespeicherter Touren. |
| `saveRoute(...)` | Mutation | `storage-service` (`POST /internal/v1/users/{userId}/saved-routes`) | Speichert aktuelle Route als Snapshot. |
| `updateSavedRoute(...)` | Mutation | `storage-service` (`PATCH /internal/v1/users/{userId}/saved-routes/{id}`) | Aktualisiert Favoritenflag oder Namen. |
| `deleteSavedRoute(id)` | Mutation | `storage-service` (`DELETE /internal/v1/users/{userId}/saved-routes/{id}`) | Entfernt Tour aus Speicher. |

### 8.2 Asynchrone AMQP-Events via RabbitMQ (`contracts/asyncapi/mail-events.yaml`)

Um Datenbanktransaktionen im `identity-service` von langsamen SMTP-Handshakes zu entkoppeln, wird der E-Mail-Versand nach dem *Transactional Event Listener*-Muster asynchron über RabbitMQ abgewickelt:

| Exchange | Routing Key | Queue | Dead-Letter-Queue | Event Payload |
| :--- | :--- | :--- | :--- | :--- |
| `warp.mail` | `mail.send` | `warp.mail.send` | `warp.mail.send.dlq` | `{ "userId": "usr_...", "mailType": "VERIFICATION \| PASSWORD_RESET", "token": "string", "timestamp": "2026-11-08T12:00:00Z" }` |
