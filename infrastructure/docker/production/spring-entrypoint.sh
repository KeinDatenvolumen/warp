#!/usr/bin/env sh
# [KI-Log Nr. KI-016] (4fd56ee) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add local databases aswell as heroku databases (WARP-002)
# [KI-Log Nr. KI-018] (73ce21c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: update Dockerfiles, unpriviliged User (WARP-002)
# [KI-Log Nr. KI-020] (3bc1abc) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: db_url conversion (WARP-002)
# [KI-Log Nr. KI-022] (f11b9ee) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: correct PostgreSQL SSL configuration (WARP-002)
# [KI-Log Nr. KI-024] (ccafe34) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: correct PostgreSQL SSL configuration for Neon (WARP-002)
# [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
set -eu

if [ -n "${DATABASE_URL:-}" ]; then
    case "$DATABASE_URL" in
        postgres://*)
            DATABASE_CONNECTION="${DATABASE_URL#postgres://}"
            ;;
        postgresql://*)
            DATABASE_CONNECTION="${DATABASE_URL#postgresql://}"
            ;;
        jdbc:postgresql://*)
            # Bereits eine JDBC-URL, beispielsweise lokal oder manuell gesetzt.
            export DB_URL="$DATABASE_URL"
            exec java -jar /app/application.jar
            ;;
        *)
            echo "Nicht unterstütztes DATABASE_URL-Format." >&2
            exit 1
            ;;
    esac

    # Format:
    # username:password@hostname:port/database
    DB_USER_INFO="${DATABASE_CONNECTION%%@*}"
    DB_HOST_AND_DATABASE="${DATABASE_CONNECTION#*@}"

    # Benutzername und Passwort trennen.
    DB_USERNAME="${DB_USER_INFO%%:*}"
    DB_PASSWORD="${DB_USER_INFO#*:}"

    if [ "$DB_USER_INFO" = "$DB_USERNAME" ]; then
        echo "DATABASE_URL enthält kein Passwort." >&2
        exit 1
    fi

    if [ "$DB_HOST_AND_DATABASE" = "$DATABASE_CONNECTION" ]; then
        echo "DATABASE_URL enthält keinen Datenbank-Host." >&2
        exit 1
    fi

    DB_URL="jdbc:postgresql://${DB_HOST_AND_DATABASE}"

    case "$DB_URL" in
        *\?*)
            DB_URL="${DB_URL}&connectTimeout=10&socketTimeout=30&tcpKeepAlive=true"
            ;;
        *)
            DB_URL="${DB_URL}&connectTimeout=10&socketTimeout=30&tcpKeepAlive=true"
            ;;
    esac

    export DB_URL
    export DB_USERNAME
    export DB_PASSWORD

    # Keine Zugangsdaten protokollieren.
    echo "Heroku-PostgreSQL-Konfiguration wurde erfolgreich vorbereitet."
else
    echo "DATABASE_URL ist nicht gesetzt. Verwende vorhandene lokale Datenbankkonfiguration."
fi

exec java -jar /app/application.jar