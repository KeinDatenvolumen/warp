#!/usr/bin/env bash
# [KI-Log Nr. KI-02] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Monorepo-Setup, Gradle & Docker (WARP-001)
set -Eeuo pipefail

: "${SERVICE_NAME:?SERVICE_NAME muss gesetzt sein}"

PROJECT_PATH=":services:${SERVICE_NAME}"
INIT_SCRIPT="/opt/warp/spring-dev.init.gradle"

boot_pid=""
compile_pid=""

cleanup() {
    trap - EXIT INT TERM

    if [[ -n "$compile_pid" ]] \
        && kill -0 "$compile_pid" 2>/dev/null; then
        kill "$compile_pid" 2>/dev/null || true
    fi

    if [[ -n "$boot_pid" ]] \
        && kill -0 "$boot_pid" 2>/dev/null; then
        kill "$boot_pid" 2>/dev/null || true
    fi

    wait 2>/dev/null || true
}

trap cleanup EXIT INT TERM

echo "Kompiliere ${SERVICE_NAME} initial ..."

gradle \
    "${PROJECT_PATH}:classes" \
    --init-script "$INIT_SCRIPT" \
    --project-cache-dir "/tmp/gradle-initial-cache" \
    --console=plain \
    --no-daemon

echo "Starte ${SERVICE_NAME} im Entwicklungsmodus ..."

gradle \
    "${PROJECT_PATH}:bootRun" \
    --init-script "$INIT_SCRIPT" \
    --project-cache-dir "/tmp/gradle-boot-cache" \
    --console=plain \
    --no-daemon &

boot_pid=$!

# bootRun erhält Zeit, die Anwendung aus den vorhandenen Klassen zu starten.
sleep 10

if ! kill -0 "$boot_pid" 2>/dev/null; then
    set +e
    wait "$boot_pid"
    exit_code=$?
    set -e

    echo "${SERVICE_NAME}: bootRun wurde während des Starts beendet."
    exit "$exit_code"
fi

echo "Starte Continuous Build für ${SERVICE_NAME} ..."

gradle \
    "${PROJECT_PATH}:classes" \
    --continuous \
    --init-script "$INIT_SCRIPT" \
    --project-cache-dir "/tmp/gradle-watch-cache" \
    --console=plain \
    --no-daemon &

compile_pid=$!

while true; do
    if ! kill -0 "$boot_pid" 2>/dev/null; then
        set +e
        wait "$boot_pid"
        exit_code=$?
        set -e

        echo "${SERVICE_NAME}: bootRun wurde beendet."
        exit "$exit_code"
    fi

    if ! kill -0 "$compile_pid" 2>/dev/null; then
        set +e
        wait "$compile_pid"
        exit_code=$?
        set -e

        echo "${SERVICE_NAME}: Continuous Build wurde beendet."
        exit "$exit_code"
    fi

    sleep 1
done