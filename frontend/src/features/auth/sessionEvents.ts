// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
type SessionInvalidListener = () => void

const listeners = new Set<SessionInvalidListener>()

export function subscribeToSessionInvalid(
    listener: SessionInvalidListener
): () => void {
    listeners.add(listener)

    return () => {
        listeners.delete(listener)
    }
}

export function notifySessionInvalid(): void {
    for (const listener of [...listeners]) {
        listener()
    }
}