# WaRP Docker-Entwicklungsmodus

Diese Dateien ergänzen das bestehende Projekt um eine vollständig getrennte lokale Entwicklungsumgebung. Die vorhandenen Produktions-Dockerfiles, `docker-compose.yaml` und Heroku-Workflows bleiben unverändert.

## Dateien übernehmen

Kopiere den Inhalt dieses Pakets in den Projektstamm. Vorhandene Dateien werden dabei nicht ersetzt. `frontend/vite.config.dev.js` ist absichtlich eine zusätzliche Vite-Konfiguration.

## Start

```bash
docker compose -f docker-compose.dev.yaml up --build
```

Danach:

- Frontend: http://localhost:5173
- BFF: http://localhost:8080
- Identity: http://localhost:8081
- Routing: http://localhost:8082
- Weather: http://localhost:8083
- Storage: http://localhost:8084

## Entwicklungsablauf

- Änderungen unter `frontend/` werden durch Vite erkannt und per HMR in den Browser übernommen.
- Änderungen an Java- oder Resource-Dateien werden durch Gradles Continuous Build kompiliert.
- Spring Boot DevTools erkennt die geänderten Klassen im Build-Verzeichnis und startet nur den betroffenen Application Context neu.
- Änderungen an `build.gradle`, `settings.gradle`, dem Gradle Wrapper oder den Dev-Dockerdateien erfordern einen Container-Neustart beziehungsweise Rebuild.

## Nützliche Befehle

Alles starten:

```bash
docker compose -f docker-compose.dev.yaml up --build
```

Im Hintergrund starten:

```bash
docker compose -f docker-compose.dev.yaml up --build -d
```

Logs eines Dienstes verfolgen:

```bash
docker compose -f docker-compose.dev.yaml logs -f weather-service
```

Nur einen Dienst und seine Abhängigkeiten starten:

```bash
docker compose -f docker-compose.dev.yaml up --build weather-service
```

Nach einer Änderung an Abhängigkeiten neu bauen:

```bash
docker compose -f docker-compose.dev.yaml up --build --force-recreate
```

Beenden:

```bash
docker compose -f docker-compose.dev.yaml down
```

Auch die lokalen Entwicklungs-Caches löschen:

```bash
docker compose -f docker-compose.dev.yaml down --volumes
```

## Deployment-Trennung

Das Deployment verwendet weiterhin ausschließlich:

- `docker-compose.yaml`
- `services/*/Dockerfile`
- `frontend/Dockerfile`

Die Entwicklungsumgebung verwendet ausschließlich:

- `docker-compose.dev.yaml`
- `infrastructure/docker/dev/Dockerfile.spring`
- `frontend/Dockerfile.dev`

Spring Boot DevTools wird nur über das lokale Gradle-Init-Script hinzugefügt. Es wird weder in die regulären `build.gradle`-Dateien noch in die produktiven JAR-Dateien aufgenommen.
