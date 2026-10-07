#!/usr/bin/env sh
set -eu

if [ -n "${DATABASE_URL:-}" ]; then
    case "$DATABASE_URL" in
        postgres://*)
            DB_URL="jdbc:postgresql://${DATABASE_URL#postgres://}"
            ;;
        postgresql://*)
            DB_URL="jdbc:postgresql://${DATABASE_URL#postgresql://}"
            ;;
        jdbc:postgresql://*)
            DB_URL="$DATABASE_URL"
            ;;
        *)
            echo "Nicht unterstütztes DATABASE_URL-Format." >&2
            exit 1
            ;;
    esac

    case "$DB_URL" in
        *\?*)
            DB_URL="${DB_URL}&sslmode=verify-full&sslrootcert=system&connectTimeout=2&socketTimeout=2&tcpKeepAlive=true"
            ;;
        *)
            DB_URL="${DB_URL}?sslmode=verify-full&sslrootcert=system&connectTimeout=2&socketTimeout=2&tcpKeepAlive=true"
            ;;
    esac

    export DB_URL
    unset DB_USERNAME
    unset DB_PASSWORT
fi

exec java -jar /app/application.jar