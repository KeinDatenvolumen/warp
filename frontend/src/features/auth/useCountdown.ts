// [KI-Log Nr. KI-125] (d8c8a50) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add login validation, error mapping, redirect target and countdown helper (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {useCallback, useEffect, useState} from 'react'

export function useCountdown() {
    const [endsAt, setEndsAt] = useState<number | null>(null)
    const [remainingSeconds, setRemainingSeconds] = useState(0)

    useEffect(() => {
        if (endsAt === null) {
            return
        }

        function tick(): void {
            const remaining = Math.max(
                0,
                Math.ceil((endsAt! - Date.now()) / 1000)
            )

            setRemainingSeconds(remaining)

            if (remaining === 0) {
                setEndsAt(null)
            }
        }

        tick()
        const intervalId = window.setInterval(tick, 1000)

        return () => {
            window.clearInterval(intervalId)
        }
    }, [endsAt])

    const start = useCallback((seconds: number) => {
        setEndsAt(Date.now() + seconds * 1000)
    }, [])

    return {remainingSeconds, start}
}