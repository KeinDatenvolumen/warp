#!/usr/bin/env sh
# [KI-Log Nr. KI-04] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): GraphQL BFF & Apollo Client (WARP-003/004)

set -eu

LOCK_FILE="/app/package-lock.json"
INSTALLED_LOCK_FILE="/app/node_modules/.warp-package-lock.json"

if [ ! -f "$LOCK_FILE" ]; then
    echo "FEHLER: package-lock.json wurde nicht gefunden."
    exit 1
fi

install_dependencies=false

if [ ! -d "/app/node_modules" ]; then
    install_dependencies=true
elif [ ! -f "$INSTALLED_LOCK_FILE" ]; then
    install_dependencies=true
elif ! cmp -s "$LOCK_FILE" "$INSTALLED_LOCK_FILE"; then
    install_dependencies=true
fi

if [ "$install_dependencies" = "true" ]; then
    echo "Frontend-Abhängigkeiten werden synchronisiert ..."

    npm ci

    cp "$LOCK_FILE" "$INSTALLED_LOCK_FILE"

    echo "Frontend-Abhängigkeiten sind aktuell."
else
    echo "Frontend-Abhängigkeiten sind bereits aktuell."
fi

exec "$@"