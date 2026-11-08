// [KI-Log Nr. KI-121] (34d35e2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: detect invalid sessions globally in the GraphQL client (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
type SessionInvalidListener = () => void
type SessionUpdatedListener = () => void

const listeners = new Set<SessionInvalidListener>()
const updatedListeners = new Set<SessionUpdatedListener>()

const SESSION_UPDATED_STORAGE_KEY = 'warp:session-updated'
const SESSION_UPDATED_CHANNEL_NAME = 'warp_session_events'

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

export function subscribeToSessionUpdated(
    listener: SessionUpdatedListener
): () => void {
    updatedListeners.add(listener)

    const handleStorage = (event: StorageEvent): void => {
        if (event.key === SESSION_UPDATED_STORAGE_KEY) {
            listener()
        }
    }

    let channel: BroadcastChannel | null = null
    if (typeof window !== 'undefined') {
        window.addEventListener('storage', handleStorage)
        if (typeof BroadcastChannel !== 'undefined') {
            try {
                channel = new BroadcastChannel(SESSION_UPDATED_CHANNEL_NAME)
                channel.onmessage = () => {
                    listener()
                }
            } catch {
                channel = null
            }
        }
    }

    return () => {
        updatedListeners.delete(listener)
        if (typeof window !== 'undefined') {
            window.removeEventListener('storage', handleStorage)
        }
        if (channel) {
            try {
                channel.close()
            } catch {
                // ignore
            }
        }
    }
}

export function notifySessionUpdated(): void {
    for (const listener of [...updatedListeners]) {
        listener()
    }

    if (typeof window !== 'undefined') {
        try {
            window.localStorage.setItem(
                SESSION_UPDATED_STORAGE_KEY,
                String(Date.now())
            )
        } catch {
            // ignore storage errors
        }

        if (typeof BroadcastChannel !== 'undefined') {
            try {
                const channel = new BroadcastChannel(
                    SESSION_UPDATED_CHANNEL_NAME
                )
                channel.postMessage('SESSION_UPDATED')
                channel.close()
            } catch {
                // ignore broadcast errors
            }
        }
    }
}