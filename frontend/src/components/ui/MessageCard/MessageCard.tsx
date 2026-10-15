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