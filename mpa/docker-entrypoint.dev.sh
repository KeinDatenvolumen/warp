#!/usr/bin/env sh
# [KI-Log Nr. KI-20] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Multi-Page Application mit Express & EJS (WARP-034)

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
    echo "MPA-Abhängigkeiten werden synchronisiert ..."
    npm ci
    cp "$LOCK_FILE" "$INSTALLED_LOCK_FILE"
    echo "MPA-Abhängigkeiten sind aktuell."
else
    echo "MPA-Abhängigkeiten sind bereits aktuell."
fi

exec "$@"
