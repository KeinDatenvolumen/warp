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