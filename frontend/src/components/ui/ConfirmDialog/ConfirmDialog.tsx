import {useEffect, useId, useRef} from 'react'
import styles from './ConfirmDialog.module.css'

interface ConfirmDialogProps {
    open: boolean
    title: string
    description: string
    confirmLabel: string
    cancelLabel?: string
    onConfirm: () => void
    onCancel: () => void
}

export default function ConfirmDialog({
                                          open,
                                          title,
                                          description,
                                          confirmLabel,
                                          cancelLabel = 'Abbrechen',
                                          onConfirm,
                                          onCancel
                                      }: ConfirmDialogProps) {
    const dialogRef = useRef<HTMLDialogElement>(null)
    const cancelButtonRef = useRef<HTMLButtonElement>(null)
    const titleId = useId()
    const descriptionId = useId()

    useEffect(() => {
        const dialog = dialogRef.current

        if (!dialog) {
            return
        }

        if (open && !dialog.open) {
            // Fallback für Umgebungen ohne showModal (z. B. Tests).
            if (typeof dialog.showModal === 'function') {
                dialog.showModal()
            } else {
                dialog.setAttribute('open', '')
            }

            // Fokus auf die ungefährliche Option, nicht auf "Bestätigen".
            cancelButtonRef.current?.focus()
        }

        if (!open && dialog.open) {
            if (typeof dialog.close === 'function') {
                dialog.close()
            } else {
                dialog.removeAttribute('open')
            }
        }
    }, [open])

    return (
        <dialog
            ref={dialogRef}
            className={styles.dialog}
            aria-labelledby={titleId}
            aria-describedby={descriptionId}
            // Esc: Der Browser schließt den Dialog selbst, wir
            // übernehmen das über den State.
            onCancel={(event) => {
                event.preventDefault()
                onCancel()
            }}
            // Klick auf den abgedunkelten Hintergrund
            onClick={(event) => {
                if (event.target === event.currentTarget) {
                    onCancel()
                }
            }}
        >
            <div className={styles.content}>
                <span className={styles.icon} aria-hidden="true">⎋</span>

                <h2 id={titleId} className={styles.title}>
                    {title}
                </h2>

                <p id={descriptionId} className={styles.description}>
                    {description}
                </p>

                <div className={styles.actions}>
                    <button
                        ref={cancelButtonRef}
                        type="button"
                        className={styles.cancelButton}
                        onClick={onCancel}
                    >
                        {cancelLabel}
                    </button>

                    <button
                        type="button"
                        className={styles.confirmButton}
                        onClick={onConfirm}
                    >
                        {confirmLabel}
                    </button>
                </div>
            </div>
        </dialog>
    )
}