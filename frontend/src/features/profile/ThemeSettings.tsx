import {useId} from 'react'
import {COLOR_MODES, COLOR_PROFILES} from '../theme/themeConfig'
import {useTheme} from '../theme/useTheme'
import styles from './Profile.module.css'

export default function ThemeSettings() {
    const {profile, mode, setProfile, setMode} = useTheme()
    const groupId = useId()

    return (
        <section
            className={styles.section}
            aria-labelledby={`${groupId}-title`}
        >
            <h2 id={`${groupId}-title`}>Darstellung</h2>
            <p className={styles.sectionDescription}>
                Die Auswahl wird auf diesem Gerät gespeichert.
            </p>

            <fieldset className={styles.fieldset}>
                <legend>Farbprofil</legend>
                <div className={styles.optionGroup}>
                    {COLOR_PROFILES.map((option) => (
                        <label key={option.id} className={styles.option}>
                            <input
                                type="radio"
                                name={`${groupId}-profile`}
                                value={option.id}
                                checked={profile === option.id}
                                onChange={() => setProfile(option.id)}
                            />
                            <span>{option.label}</span>
                        </label>
                    ))}
                </div>
            </fieldset>

            <fieldset className={styles.fieldset}>
                <legend>Modus</legend>
                <div className={styles.optionGroup}>
                    {COLOR_MODES.map((option) => (
                        <label key={option.id} className={styles.option}>
                            <input
                                type="radio"
                                name={`${groupId}-mode`}
                                value={option.id}
                                checked={mode === option.id}
                                onChange={() => setMode(option.id)}
                            />
                            <span>{option.label}</span>
                        </label>
                    ))}
                </div>
            </fieldset>
        </section>
    )
}