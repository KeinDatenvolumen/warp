#!/usr/bin/env bash
# [KI-Log Nr. KI-02] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Monorepo-Setup, Gradle & Docker (WARP-001)

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIRECTORY="${PROJECT_ROOT}/logs/local"

GRADLE_PIDS=()
FRONTEND_PID=""

cd "$PROJECT_ROOT"

print_heading() {
    echo
    echo "============================================================"
    echo "$1"
    echo "============================================================"
}

require_command() {
    local command_name="$1"
    local installation_hint="$2"

    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "FEHLER: Der Befehl '${command_name}' wurde nicht gefunden."
        echo "$installation_hint"
        exit 1
    fi
}

check_port() {
    local port="$1"
    local service_name="$2"

    if command -v lsof >/dev/null 2>&1; then
        if lsof -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
            echo "FEHLER: Port ${port} wird bereits verwendet."
            echo "Betroffener Dienst: ${service_name}"
            echo
            echo "Pruefe den belegenden Prozess mit:"
            echo "  lsof -iTCP:${port} -sTCP:LISTEN"
            exit 1
        fi
    elif command -v ss >/dev/null 2>&1; then
        if ss -ltn | grep -qE ":${port}[[:space:]]"; then
            echo "FEHLER: Port ${port} wird bereits verwendet."
            echo "Betroffener Dienst: ${service_name}"
            exit 1
        fi
    fi
}

start_backend_service() {
    local service_name="$1"
    local gradle_project="$2"
    local log_file="${LOG_DIRECTORY}/${service_name}.log"

    echo "Starte ${service_name} ..."

    ./gradlew \
        ":services:${gradle_project}:bootRun" \
        --console=plain \
        >"$log_file" 2>&1 &

    local process_id=$!
    GRADLE_PIDS+=("$process_id")

    echo "  PID: ${process_id}"
    echo "  Log: ${log_file}"
}

wait_for_health() {
    local service_name="$1"
    local health_url="$2"
    local attempts=30

    for attempt in $(seq 1 "$attempts"); do
        if curl \
            --fail \
            --silent \
            --show-error \
            --max-time 2 \
            "$health_url" \
            >/dev/null 2>&1; then
            printf "%-20s UP\n" "$service_name"
            return 0
        fi

        sleep 2
    done

    printf "%-20s FEHLER\n" "$service_name"
    echo
    echo "${service_name} wurde nicht rechtzeitig bereit."
    echo "Pruefe die Logs unter:"
    echo "  ${LOG_DIRECTORY}"
    return 1
}

cleanup() {
    local exit_code=$?

    trap - EXIT INT TERM

    echo
    echo "Beende lokale WaRP-Prozesse ..."

    if [[ -n "$FRONTEND_PID" ]] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi

    for process_id in "${GRADLE_PIDS[@\]:-}"; do
        if kill -0 "$process_id" 2>/dev/null; then
            kill "$process_id" 2>/dev/null || true
        fi
    done

    wait 2>/dev/null || true

    echo "Alle durch dieses Skript gestarteten Prozesse wurden beendet."

    exit "$exit_code"
}

trap cleanup EXIT INT TERM

print_heading "WaRP lokale Entwicklungsumgebung"

require_command "java" \
    "Installiere ein JDK 21 und stelle sicher, dass JAVA_HOME korrekt gesetzt ist."

require_command "node" \
    "Installiere Node.js 22.12.0 oder neuer."

require_command "npm" \
    "npm wird zusammen mit Node.js installiert."

require_command "curl" \
    "Installiere curl, damit die Healthchecks ausgefuehrt werden koennen."

if [[ ! -f "${PROJECT_ROOT}/gradle/wrapper/gradle-wrapper.jar" ]]; then
    echo "FEHLER: gradle/wrapper/gradle-wrapper.jar fehlt."
    echo "Die Gradle-Wrapper-Dateien muessen im Repository enthalten sein."
    exit 1
fi

if [[ ! -x "${PROJECT_ROOT}/gradlew" ]]; then
    chmod +x "${PROJECT_ROOT}/gradlew"
fi

print_heading "Installierte Versionen"

java -version
echo
node --version
npm --version
echo
./gradlew --version

print_heading "Ports pruefen"

check_port 8080 "BFF-Service"
check_port 8081 "Identity-Service"
check_port 8082 "Routing-Service"
check_port 8083 "Weather-Service"
check_port 8084 "Storage-Service"
check_port 5173 "React-Frontend"

echo "Alle benoetigten Ports sind frei."

mkdir -p "$LOG_DIRECTORY"

if [[ ! -d "${PROJECT_ROOT}/frontend/node_modules" ]]; then
    print_heading "Frontend-Abhaengigkeiten installieren"

    (
        cd "${PROJECT_ROOT}/frontend"
        npm ci
    )
fi

print_heading "Backend-Services starten"

start_backend_service "bff-service" "bff-service"
start_backend_service "identity-service" "identity-service"
start_backend_service "routing-service" "routing-service"
start_backend_service "weather-service" "weather-service"
start_backend_service "storage-service" "storage-service"

print_heading "Frontend starten"

(
    cd "${PROJECT_ROOT}/frontend"
    npm run dev
) >"${LOG_DIRECTORY}/frontend.log" 2>&1 &

FRONTEND_PID=$!

echo "Frontend gestartet."
echo "  PID: ${FRONTEND_PID}"
echo "  Log: ${LOG_DIRECTORY}/frontend.log"

print_heading "Auf Services warten"

health_failed=false

wait_for_health \
    "BFF-Service" \
    "http://localhost:8080/actuator/health/readiness" \
    || health_failed=true

wait_for_health \
    "Identity-Service" \
    "http://localhost:8081/actuator/health/readiness" \
    || health_failed=true

wait_for_health \
    "Routing-Service" \
    "http://localhost:8082/actuator/health/readiness" \
    || health_failed=true

wait_for_health \
    "Weather-Service" \
    "http://localhost:8083/actuator/health/readiness" \
    || health_failed=true

wait_for_health \
    "Storage-Service" \
    "http://localhost:8084/actuator/health/readiness" \
    || health_failed=true

if [[ "$health_failed" == true ]]; then
    echo
    echo "Mindestens ein Service konnte nicht gestartet werden."
    echo "Pruefe die Logdateien unter:"
    echo "  ${LOG_DIRECTORY}"
    exit 1
fi

print_heading "WaRP wurde erfolgreich gestartet"

echo "Frontend:"
echo "  http://localhost:5173"
echo
echo "BFF-Gesamtstatus:"
echo "  http://localhost:8080/api/v1/system/health"
echo
echo "Einzelne Readiness-Endpunkte:"
echo "  http://localhost:8080/actuator/health/readiness"
echo "  http://localhost:8081/actuator/health/readiness"
echo "  http://localhost:8082/actuator/health/readiness"
echo "  http://localhost:8083/actuator/health/readiness"
echo "  http://localhost:8084/actuator/health/readiness"
echo
echo "Logs:"
echo "  ${LOG_DIRECTORY}"
echo
echo "Druecke Strg+C, um alle gestarteten Prozesse zu beenden."

while true; do
    sleep 5

    for process_id in "${GRADLE_PIDS[@]}"; do
        if ! kill -0 "$process_id" 2>/dev/null; then
            echo
            echo "Ein Backend-Prozess wurde unerwartet beendet."
            echo "Pruefe die Logs unter ${LOG_DIRECTORY}."
            exit 1
        fi
    done

    if ! kill -0 "$FRONTEND_PID" 2>/dev/null; then
        echo
        echo "Das Frontend wurde unerwartet beendet."
        echo "Pruefe ${LOG_DIRECTORY}/frontend.log."
        exit 1
    fi
done