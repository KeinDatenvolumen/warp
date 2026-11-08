<!-- [KI-Log Nr. KI-033] (7bdabfa) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add graphql to backend and frontend. Initialize dict for SPA (WARP-003) -->
<!-- [KI-Log Nr. KI-061] (39758b1) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: define activity profile GraphQL contract (WARP-011) -->
<!-- [KI-Log Nr. KI-074] (cfd4246) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: expose individual activity profile query (WARP-033) -->
<!-- [KI-Log Nr. KI-145] (821b60d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: hide weather query interval from users and use km (hiking 2, bicycle 5, motorcycle 15) (WARP-033) -->
<!-- [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034) -->
# WaRP GraphQL-Vertrag

Dieses Verzeichnis enthält den öffentlichen GraphQL-Vertrag zwischen dem
React-Frontend und dem BFF-Service.

Der BFF ist der einzige GraphQL-Server. Die internen Services stellen ihre
Funktionen weiterhin über interne REST-Schnittstellen bereit.

## Verbindliche Quelle

Die Dateien unter `contracts/graphql` sind die verbindliche Definition des
öffentlichen GraphQL-Schemas.

Während des BFF-Builds werden die Schema-Dateien in die Ressourcen des
BFF-Service kopiert.

## Namenskonventionen

### Queries

Queries verwenden beschreibende Substantive oder Substantivgruppen in
`camelCase`.

Beispiele:

- `currentUser`
- `systemHealth`
- `activityProfiles`
- `routeCalculation`
- `savedRoutes`

Queries erhalten kein Präfix wie `get`, `find` oder `fetch`.

### Mutations

Mutations beginnen mit einem Verb und verwenden `camelCase`.

Beispiele:

- `updateCurrentUser`
- `startRouteCalculation`
- `saveRoute`
- `deleteSavedRoute`

### Objekt- und Rückgabetypen

GraphQL-Typen verwenden `PascalCase`.

Beispiele:

- `User`
- `SystemHealth`
- `RouteCalculation`
- `SavedRoutePage`

Queries geben fachliche Objekttypen direkt zurück.

Mutations geben einen eigenen Payload-Typ zurück.

Beispiel:

```graphql
updateCurrentUser(
    input: UpdateCurrentUserInput!
): UpdateCurrentUserPayload!
```

### Input-Typen

Input-Typen verwenden `PascalCase` und enden mit `Input`.

Beispiele:

- `UpdateCurrentUserInput`
- `RouteCalculationInput`
- `PageInput`

Parameter mit mehreren zusammengehörenden Feldern werden in einem Input-Typ
gebündelt.

### Mutation-Payloads

Payload-Typen verwenden den vollständigen Mutationsnamen und enden mit
`Payload`.

Beispiele:

- `UpdateCurrentUserPayload`
- `StartRouteCalculationPayload`
- `DeleteSavedRoutePayload`

Payloads enthalten das Ergebnis sowie erwartbare feldbezogene Fehler.

```graphql
type UpdateCurrentUserPayload {
    user: User
    errors: [FieldError!]!
}
```

### Enums

Enum-Typen verwenden `PascalCase`.

Enum-Werte verwenden ausschließlich `UPPER_SNAKE_CASE`.

```graphql
enum ActivityProfileCode {
    BICYCLE
    MOTORCYCLE
    HIKING
}
```

### IDs

IDs haben den GraphQL-Typ `ID!`.

Das Präfix ist Bestandteil des Werts, beispielsweise:

- `usr_123`
- `calc_123`
- `route_123`

Clients dürfen IDs nicht analysieren oder selbst erzeugen.

### Listen

Listen, die bei fehlenden Ergebnissen leer sein können, sind nicht nullable:

```graphql
activityProfiles: [ActivityProfile!]!
```

Es wird eine leere Liste statt `null` zurückgegeben.

### Datums- und Zeitwerte

Zeitpunkte verwenden den Scalar `DateTime` und werden als ISO-8601-Werte
übertragen.

Beispiel:

```text
2026-10-08T19:01:35Z
```

### Fehler

Erwartbare feldbezogene Fehler einer Mutation werden über das Feld `errors`
des Payload-Typs übertragen.

Operationsweite Fehler werden über das standardisierte GraphQL-Feld
`errors[]` zurückgegeben.

Jeder operationsweite Fehler enthält einen stabilen Maschinen-Code:

```json
{
   "errors": [
      {
         "message": "Der Dienst ist derzeit nicht verfügbar.",
         "extensions": {
            "code": "SERVICE_UNAVAILABLE"
         }
      }
   ]
}
```

Stacktraces, interne Klassennamen, interne URLs und Fehlermeldungen
nachgelagerter Dienste werden nicht an Clients weitergegeben.

## Fehlerkategorien und stabile Fehlercodes

Der BFF unterscheidet zwischen technischen, allgemeinen und fachlichen
Fehlercodes.

Allgemeine technische Fehlercodes:

- `BAD_REQUEST`
- `VALIDATION_FAILED`
- `NOT_AUTHENTICATED`
- `ACCESS_DENIED`
- `RESOURCE_NOT_FOUND`
- `DOWNSTREAM_SERVICE_UNAVAILABLE`
- `DOWNSTREAM_SERVICE_ERROR`
- `GRAPHQL_EXECUTION_ERROR`
- `INTERNAL_SERVER_ERROR`

Fachliche Fehlercodes werden pro Anwendungsfall definiert. Beispiele:

- `SESSION_INVALID`
- `ACTIVITY_PROFILE_NOT_FOUND`
- `GEOCODING_SERVICE_UNAVAILABLE`
- `ROUTE_CALCULATION_NOT_FOUND`
- `WEATHER_EVALUATION_NOT_FOUND`
- `SAVED_ROUTE_NOT_FOUND`
- `INVALID_ROUTE_REQUEST`
- `EMAIL_NOT_VERIFIED`

Fehlercodes interner oder externer Dienste werden nicht ungeprüft an das
Frontend weitergegeben. Der BFF übersetzt sie in stabile WaRP-Fehlercodes.

## Fehlerbehandlung bei Mutations

Erwartbare Validierungs- und Fachfehler stehen im `errors`-Feld des jeweiligen
Mutation-Payloads.

Beispiel:

```graphql
type UpdateCurrentUserPayload {
    user: User
    errors: [FieldError!]!
}

type FieldError {
    field: String
    code: String!
    message: String!
}
```

Ein Fehler kann einem konkreten Eingabefeld zugeordnet werden:

```json
{
   "data": {
      "updateCurrentUser": {
         "user": null,
         "errors": [
            {
               "field": "preferredActivityProfile",
               "code": "INVALID_ACTIVITY_PROFILE",
               "message": "Das Aktivitätsprofil ist ungültig."
            }
         ]
      }
   }
}
```

Operationsweite Fehler werden über das standardisierte GraphQL-Fehlerfeld
zurückgegeben:

```json
{
   "data": {
      "savedRoute": null
   },
   "errors": [
      {
         "message": "Die gespeicherte Route wurde nicht gefunden.",
         "path": ["savedRoute"],
         "extensions": {
            "code": "SAVED_ROUTE_NOT_FOUND"
         }
      }
   ]
}
```

## Transport

Der GraphQL-Endpunkt des BFF lautet:

```text
POST /graphql
```

Requests verwenden:

```http
Content-Type: application/json
Accept: application/graphql-response+json
```

Ein Request besitzt grundsätzlich folgende JSON-Struktur:

```json
{
   "query": "query SystemHealth { systemHealth { status } }",
   "operationName": "SystemHealth",
   "variables": {}
}
```

Das React-Frontend kommuniziert ausschließlich mit dem BFF. Interne Services
werden niemals direkt durch den Browser aufgerufen.

## Authentifizierung und CSRF

GraphQL verwendet dieselbe Session wie die REST-Endpunkte des BFF.

Das Session-Cookie wird vom Browser automatisch an den BFF gesendet. Für
zustandsverändernde GraphQL-Mutationen wird zusätzlich ein CSRF-Token
verwendet.

Anmelde- und Abmeldevorgänge bleiben REST-Endpunkte, weil dort das sichere
Session-Cookie gesetzt beziehungsweise gelöscht wird.

## Erste GraphQL-Query

Die erste GraphQL-Query des BFF lautet:

```graphql
query SystemHealth {
    systemHealth {
        status
        checkedAt
        healthyServices
        totalServices
        services {
            service
            status
            reachable
            responseTimeMs
        }
    }
}
```

Die Query liefert den zusammengefassten Zustand des BFF und der internen
Services. Der BFF ruft dafür die Readiness-Endpunkte der internen Services über
REST auf.

Interne URLs wie die konkreten Readiness-Adressen werden nicht in das
öffentliche GraphQL-Schema aufgenommen.

## Schema-Struktur

Die verbindliche Schema-Datei liegt unter:

```text
contracts/graphql/schema.graphqls
```

Der BFF übernimmt die Schema-Datei während des Gradle-Builds nach:

```text
build/resources/main/graphql/schema.graphqls
```

Spring Boot lädt das GraphQL-Schema beim Start aus den Anwendungsressourcen.
Wenn das Schema ungültig ist oder implementierte Resolver nicht zum Vertrag
passen, muss der Build beziehungsweise der Anwendungstest fehlschlagen.

## Lokale Prüfung

Unter Windows kann die Verarbeitung der BFF-Ressourcen so geprüft werden:

```powershell
.\gradlew.bat :services:bff-service:processResources
```

Die kopierte Schema-Datei kann anschließend angezeigt werden:

```powershell
Get-Content `
  services\bff-service\build\resources\main\graphql\schema.graphqls
```

Die BFF-Tests werden so ausgeführt:

```powershell
.\gradlew.bat :services:bff-service:test
```

Nach der Implementierung des GraphQL-Endpunkts kann die Query lokal getestet
werden:

```powershell
$Body = @{
    query = @"
query SystemHealth {
    systemHealth {
        status
        checkedAt
        healthyServices
        totalServices
        services {
            service
            status
            reachable
            responseTimeMs
        }
    }
}
"@
    operationName = "SystemHealth"
    variables = @{}
} | ConvertTo-Json -Depth 10

Invoke-RestMethod `
    -Uri "http://localhost:8080/graphql" `
    -Method Post `
    -ContentType "application/json" `
    -Headers @{ Accept = "application/graphql-response+json" } `
    -Body $Body
```

## Pflege des Vertrags

Bei Änderungen an der GraphQL-API gelten folgende Regeln:

1. Der Vertrag unter `contracts/graphql` wird zuerst angepasst.
2. Schema und BFF-Implementierung müssen konsistent bleiben.
3. Neue Queries und Mutations erhalten automatisierte Tests.
4. Änderungen dürfen bestehende Clients nicht unbeabsichtigt brechen.
5. Entfernte Felder werden zunächst als veraltet markiert.
6. Interne Serviceadressen und technische Details werden nicht veröffentlicht.
7. Neue Fehlercodes werden dokumentiert und bleiben nach Veröffentlichung
   stabil.
8. Das Frontend verwendet benannte GraphQL-Operationen.
9. GraphQL-Requests werden ausschließlich an den BFF gesendet.

Veraltete Felder werden mit `@deprecated` gekennzeichnet:

```graphql
type Example {
    oldField: String @deprecated(reason: "Verwende newField.")
    newField: String
}
```

## Schema-Bereiche

Das Schema wird schrittweise um folgende fachliche Bereiche erweitert:

- Benutzer und aktuelle Sitzung
- Aktivitätsprofile
- Geocodierung
- Routenberechnung
- Wetterauswertung
- Abfahrtsalternativen
- gespeicherte Routen

### Aktivitätsprofile

Die zentral konfigurierten Aktivitätsprofile sind:

- `BICYCLE`
- `MOTORCYCLE`
- `HIKING`

Intern, im Routing-Service, enthält ein Aktivitätsprofil:

- einen stabilen fachlichen Profilcode,
- einen Anzeigenamen,
- eine zentrale Durchschnittsgeschwindigkeit,
- ein zugeordnetes Routingprofil,
- ein Wetter-Abfrageintervall in Kilometern,
- zentrale Wegepräferenzen.

Über die öffentliche GraphQL-API werden ausschließlich der Profilcode und der
Anzeigename ausgeliefert. Alle übrigen Profilparameter sind rein intern und
werden dem Frontend nicht angezeigt.

Die Profilparameter werden zentral im Backend verwaltet. Endbenutzer dürfen
ausschließlich ein Profil auswählen. Die Parameter eines Profils, beispielsweise
die Geschwindigkeit, das Wetter-Abfrageintervall oder die Wegepräferenzen,
sind weder sichtbar noch über die öffentliche GraphQL-API veränderbar.

Das Wetter-Abfrageintervall legt fest, in welchem Streckenabstand entlang
der Route Wetterdaten abgefragt werden:

| Profil        | Code         | Wetter-Abfrageintervall |
|---------------|--------------|-------------------------|
| Wandern       | `HIKING`     | 2 km                    |
| Fahrrad       | `BICYCLE`    | 5 km                    |
| Motorrad      | `MOTORCYCLE` | 15 km                   |

Die verfügbaren Profile werden über folgende Query geliefert:

```graphql
query ActivityProfiles {
    activityProfiles {
        code
        displayName
    }
}
```

Ein einzelnes Aktivitätsprofil wird über seinen Profilcode geladen:

```graphql
query GetActivityProfile($code: ActivityProfileCode!) {
    activityProfile(code: $code) {
        code
        displayName
    }
}
```

Beispielvariablen:

```json
{
  "code": "BICYCLE"
}
```

Die konkreten Werte der Profile werden zentral durch den Routing-Service
bereitgestellt. Der BFF gibt davon nur Profilcode und Anzeigename an das
Frontend weiter. Das Frontend darf keine eigenen Profilparameter erzeugen oder
überschreiben.

Bei der Routenberechnung (`startRouteCalculation`) und der Routenspeicherung
(`saveRoute`) wird jeweils ein unveränderlicher Snapshot des gewählten
Aktivitätsprofils an das Backend-Benutzerkonto (`userId` aus der validierten
Sitzung) gebunden und persistiert.

### Übersicht aller produktiven GraphQL-Operationen

#### Queries (`type Query`)
- `systemHealth: SystemHealth!` – Aggregierter Gesundheitsstatus aller 5 Backend-Services
- `currentSession: CurrentSession!` – Aktuell authentifizierte Sitzung (`userId`, `createdAt`, `expiresAt`)
- `currentUser: User` – Kontodaten des angemeldeten Benutzers inkl. `emailVerified`-Status
- `activityProfiles: [ActivityProfile!]!` – Alle zentral konfigurierten Aktivitätsprofile (`BICYCLE`, `MOTORCYCLE`, `HIKING`)
- `activityProfile(code: ActivityProfileCode!): ActivityProfile!` – Einzelnes Aktivitätsprofil anhand seines Codes
- `searchPlaces(query: String!, limit: Int = 5): [Place!]!` – Geocodierung / Ortssuche über den Routing-Service (mit 24h Caffeine-Cache)
- `routeCalculation(id: ID!): RouteCalculation` – Status (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`), Geometrie und verknüpfte Wetterbewertung einer gestarteten Routenberechnung
- `savedRoutes(filter: SavedRouteFilter, page: PageInput): SavedRoutePage!` – Paginierte Liste eigener gespeicherter Routen (Favoriten zuerst)
- `savedRoute(id: ID!): SavedRoute` – Vollständige Detailansicht einer gespeicherten Route samt Wettersegmenten

#### Mutations (`type Mutation`)
- `startRouteCalculation(input: RouteCalculationInput!): RouteCalculationPayload!` – Startet asynchron eine Routenberechnung inkl. anschließender Wetterbewertung
- `saveRoute(input: SaveRouteInput!): SaveRoutePayload!` – Persistiert eine berechnete Route samt Wetterbewertung im Storage-Service
- `updateSavedRoute(id: ID!, input: UpdateSavedRouteInput!): UpdateSavedRoutePayload!` – Aktualisiert Name und/oder Favoritenstatus einer eigenen Route
- `deleteSavedRoute(id: ID!): DeleteSavedRoutePayload!` – Löscht eine eigene gespeicherte Route

Die Bereiche sind Teil eines gemeinsamen öffentlichen GraphQL-Schemas des
BFF (`contracts/graphql/schema.graphqls`). Die internen Services bleiben reine
REST-/JSON-RPC-Dienste und werden nicht selbst zu öffentlichen GraphQL-Servern.

## Zentrale Fehlerübersetzung im BFF

Exceptions aus GraphQL-Resolvern werden zentral durch den BFF verarbeitet.

Der BFF stellt sicher, dass operationsweite Fehler mindestens folgende
Struktur besitzen:

```json
{
  "message": "Ein interner Fehler ist aufgetreten.",
  "path": [
    "systemHealth"
  ],
  "extensions": {
    "code": "INTERNAL_SERVER_ERROR"
  }
}
```