# WaRP – Weather-aware Route Planning

WaRP ist eine mobile Webanwendung zur wetterabhängigen Planung von Routen
für Fahrrad, Motorrad und Wandern.

## Projektstatus

Das Projekt befindet sich in Modul 0: Projektfundament und Monorepo.

Aktuell umgesetzt:

- Gradle-Multi-Project-Build
- vorbereitete Monorepo-Struktur
- minimaler Spring-Boot-BFF-Service
- Health- und Readiness-Endpunkte
- erster Spring-Kontext-Test

## Verwendete Versionen

- Java 25 LTS
- Spring Boot 4.1.1
- Gradle 9.8.0

## Projektstruktur

- `contracts`: GraphQL-, OpenAPI- und Beispielverträge
- `frontend`: React-Frontend
- `services`: fünf getrennte Spring-Boot-Services
- `infrastructure`: Docker- und Heroku-Dateien
- `scripts`: Hilfsskripte

## BFF lokal testen

Unter Linux, macOS, WSL oder Git Bash:

```bash
./gradlew :services:bff-service:test
./gradlew :services:bff-service:bootRun