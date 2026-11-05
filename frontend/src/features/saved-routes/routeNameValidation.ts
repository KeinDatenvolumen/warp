// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
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