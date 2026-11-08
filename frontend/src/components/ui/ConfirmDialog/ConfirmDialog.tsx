// [KI-Log Nr. KI-127] (2b97154) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add logout action with loading state, error handling and redirect (WARP-006)
// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {useEffect, useId, useRef} from 'react'
import styles from './ConfirmDialog.module.css'

interface ConfirmDialogProps {
    icon?: string
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
                                          icon = '⎋',
                                          onConfirm,
                                          onCancel,
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

        if (!open) {
            if (dialog.open) {
                if (typeof dialog.close === 'function') {
                    dialog.close()
                } else {
                    dialog.removeAttribute('open')
                }
            }

            return
        }

        if (!dialog.open) {
            /*
             * Fallback für Umgebungen ohne showModal,
             * beispielsweise jsdom in den Tests.
             */
            if (typeof dialog.showModal === 'function') {
                dialog.showModal()
            } else {
                dialog.setAttribute('open', '')
            }
        }

        /*
         * Die mobile Navigation wird beim Öffnen des Dialogs ebenfalls
         * geschlossen und stellt ihren Fokus auf den Menübutton zurück.
         * Die verzögerte Fokussetzung sorgt dafür, dass anschließend
         * zuverlässig die ungefährliche Dialogoption fokussiert ist.
         */
        const focusTimer = window.setTimeout(() => {
            if (dialog.open) {
                cancelButtonRef.current?.focus()
            }
        }, 0)

        return () => {
            window.clearTimeout(focusTimer)
        }
    }, [open])

    return (
        <dialog
            ref={dialogRef}
            className={styles.dialog}
            aria-labelledby={titleId}
            aria-describedby={descriptionId}
            onCancel={(event) => {
                event.preventDefault()
                onCancel()
            }}
            onClick={(event) => {
                if (event.target === event.currentTarget) {
                    onCancel()
                }
            }}
        >
            <div className={styles.content}>
                <span
                    className={styles.icon}
                    aria-hidden="true"
                >
                    {icon}
                </span>

                <h2
                    id={titleId}
                    className={styles.title}
                >
                    {title}
                </h2>

                <p
                    id={descriptionId}
                    className={styles.description}
                >
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