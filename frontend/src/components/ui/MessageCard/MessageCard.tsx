// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import type {PropsWithChildren} from 'react'
import styles from './MessageCard.module.css'

interface MessageCardProps extends PropsWithChildren {
    error?: boolean
}

export default function MessageCard({
                                        children,
                                        error = false
                                    }: MessageCardProps) {
    return (
        <section
            className={`${styles.card} ${error ? styles.error : ''}`}
            role={error ? 'alert' : undefined}
        >
            {children}
        </section>
    )
}