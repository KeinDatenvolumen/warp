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