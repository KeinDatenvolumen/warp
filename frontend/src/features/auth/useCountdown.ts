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