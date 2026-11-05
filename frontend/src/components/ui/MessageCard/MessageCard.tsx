// [KI-Log Nr. KI-02] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Monorepo-Setup, Gradle & Docker (WARP-001)
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