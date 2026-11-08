#!/usr/bin/env bash
# [KI-Log Nr. KI-002] (8c3033f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize WaRP monorepo and BFF service (WARP-001)
# [KI-Log Nr. KI-003] (39894d5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize Deployment and Health Endpoints for Services. Frontend to examine health status of services (WARP-001)
# [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT="${PROJECT_ROOT}/dump.txt"

cd "$PROJECT_ROOT"

EXCLUDED_DIRS=(
    ".git"
    ".gradle"
    ".idea"
    ".vscode"
    "node_modules"
    "build"
    "target"
    "out"
    "dist"
    "coverage"
    "logs"
    "tmp"
    "temp"
)

is_excluded_path() {
    local path="$1"

    for directory in "${EXCLUDED_DIRS[@]}"; do
        if [[ "$path" == "./${directory}" ]] ||
           [[ "$path" == "./${directory}/"* ]] ||
           [[ "$path" == *"/${directory}/"* ]]; then
            return 0
        fi
    done

    return 1
}

is_sensitive_file() {
    local path="$1"
    local filename
    filename="$(basename "$path")"

    case "$filename" in
        .env|.env.*|*.pem|*.key|*.p12|*.pfx|*.jks|*.keystore)
            if [[ "$filename" == ".env.example" ]]; then
                return 1
            fi
            return 0
            ;;
        credentials*|secrets*|secret*)
            return 0
            ;;
        application-local.yml|application-local.yaml|application-local.properties)
            return 0
            ;;
    esac

    return 1
}

is_ignored_file() {
    local path="$1"
    local filename
    filename="$(basename "$path")"

    case "$filename" in
        "$OUTPUT"|dump.txt|*.log|*.class|*.jar|*.war|*.zip|*.tar|*.gz|*.7z)
            return 0
            ;;
        *.png|*.jpg|*.jpeg|*.gif|*.webp|*.ico|*.pdf|*.doc|*.docx|*.xls|*.xlsx)
            return 0
            ;;
        *.db|*.sqlite|*.sqlite3)
            return 0
            ;;
        .DS_Store)
            return 0
            ;;
    esac

    return 1
}

: > "$OUTPUT"

{
    echo "=========================================="
    echo "WARP-PROJEKTDUMP"
    echo "Erstellt am: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    echo "=========================================="
    echo
    echo "PROJEKTSTRUKTUR"
    echo "=========================================="
} >> "$OUTPUT"

if command -v tree >/dev/null 2>&1; then
    tree -a \
        -I '.git|.gradle|.idea|.vscode|node_modules|build|target|out|dist|coverage|logs|tmp|temp|dump.txt' \
        >> "$OUTPUT"
else
    find . -print |
    while IFS= read -r path; do
        if ! is_excluded_path "$path"; then
            printf '%s\n' "$path"
        fi
    done >> "$OUTPUT"
fi

{
    echo
    echo
    echo "=========================================="
    echo "DATEI-INHALTE"
    echo "=========================================="
} >> "$OUTPUT"

find . -type f -print0 |
while IFS= read -r -d '' file; do
    if is_excluded_path "$file"; then
        continue
    fi

    if is_sensitive_file "$file"; then
        printf 'Übersprungen, potenziell sensibel: %s\n' "$file" >&2
        continue
    fi

    if is_ignored_file "$file"; then
        continue
    fi

    if [[ "$file" == "./dump.txt" ]]; then
        continue
    fi

    if grep -Iq . "$file" 2>/dev/null; then
        {
            echo
            echo
            echo "--- FILE: ${file#./} ---"
            cat "$file"
        } >> "$OUTPUT"
    fi
done

{
    echo
    echo
    echo "=========================================="
    echo "DUMP ABGESCHLOSSEN"
    echo "=========================================="
} >> "$OUTPUT"

echo "Fertig. Der Projektdump wurde erstellt:"
echo "$OUTPUT"
echo
echo "WICHTIG:"
echo "Pruefe dump.txt vor dem Weitergeben auf Zugangsdaten, Tokens und persoenliche Daten."