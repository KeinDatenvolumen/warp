// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export const MAX_ROUTE_NAME_LENGTH = 100

export function validateRouteName(value: string): string | undefined {
    const trimmed = value.trim()

    if (trimmed.length === 0) {
        return 'Bitte gib einen Namen für die Route ein.'
    }

    if (trimmed.length > MAX_ROUTE_NAME_LENGTH) {
        return `Der Name darf höchstens ${MAX_ROUTE_NAME_LENGTH} Zeichen lang sein.`
    }

    return undefined
}