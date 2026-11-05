// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Profilseite & Theme-System (WARP-033)
import {useId} from 'react'
import {
    COLOR_MODES,
    COLOR_PROFILES,
    type ColorModePreference,
    type ColorProfileId,
} from '../theme/themeConfig'
import {useTheme} from '../theme/useTheme'
import styles from './Profile.module.css'

const PROFILE_COLORS: Record<ColorProfileId, readonly string[]> = {
    'profile-1': [
        '#ea4801',
        '#f87c07',
        '#ffb800',
        '#95b734',
        '#559e2c',
    ],
    'profile-2': [
        '#4a5a6a',
        '#a07b5c',
        '#e6d8c6',
        '#2f3133',
        '#7b8a74',
    ],
    'profile-3': [
        '#2c2f2b',
        '#5b6359',
        '#8d9a8a',
        '#c8c2b5',
        '#f1eee6',
    ],
}

const MODE_SYMBOLS: Record<ColorModePreference, string> = {
    light: '☀',
    dark: '☾',
    system: '◐',
}

export default function ThemeSettings() {
    const {
        profile,
        mode,
        resolvedMode,
        setProfile,
        setMode,
    } = useTheme()

    const groupId = useId()

    return (
        <section
            className={styles.section}
            aria-labelledby={`${groupId}-title`}
        >
            <h2 id={`${groupId}-title`}>Darstellung</h2>

            <p className={styles.sectionDescription}>
                Wähle ein Farbschema und die gewünschte Darstellung.
                Die Auswahl wird sofort angewendet und auf diesem Gerät
                gespeichert.
            </p>

            <fieldset className={styles.fieldset}>
                <legend>Farbschema</legend>

                <div className={styles.profileOptionGroup}>
                    {COLOR_PROFILES.map((option) => {
                        const selected = profile === option.id

                        return (
                            <label
                                key={option.id}
                                className={styles.profileOption}
                            >
                                <input
                                    className={styles.optionInput}
                                    type="radio"
                                    name={`${groupId}-profile`}
                                    value={option.id}
                                    aria-label={option.label}
                                    checked={selected}
                                    onChange={() =>
                                        setProfile(option.id)
                                    }
                                />

                                <span
                                    className={styles.colorPreview}
                                    aria-hidden="true"
                                >
                                    {PROFILE_COLORS[option.id].map(
                                        (color) => (
                                            <span
                                                key={color}
                                                style={{
                                                    backgroundColor:
                                                    color,
                                                }}
                                            />
                                        ),
                                    )}
                                </span>

                                <span
                                    className={
                                        styles.profileOptionContent
                                    }
                                >
                                    <span
                                        className={
                                            styles.optionHeading
                                        }
                                    >
                                        {option.label}
                                    </span>

                                    <span
                                        className={
                                            styles.optionDescription
                                        }
                                    >
                                        {option.description}
                                    </span>
                                </span>

                                <span
                                    className={styles.selectionMark}
                                    aria-hidden="true"
                                >
                                    {selected ? '✓' : ''}
                                </span>
                            </label>
                        )
                    })}
                </div>
            </fieldset>

            <fieldset className={styles.fieldset}>
                <legend>Darstellungsmodus</legend>

                <div className={styles.modeOptionGroup}>
                    {COLOR_MODES.map((option) => (
                        <label
                            key={option.id}
                            className={styles.modeOption}
                        >
                            <input
                                className={styles.optionInput}
                                type="radio"
                                name={`${groupId}-mode`}
                                value={option.id}
                                aria-label={option.label}
                                checked={mode === option.id}
                                onChange={() => setMode(option.id)}
                            />

                            <span
                                className={styles.modeSymbol}
                                aria-hidden="true"
                            >
                                {MODE_SYMBOLS[option.id]}
                            </span>

                            <span className={styles.modeOptionContent}>
                                <span
                                    className={styles.optionHeading}
                                >
                                    {option.label}
                                </span>

                                <span
                                    className={
                                        styles.optionDescription
                                    }
                                >
                                    {option.description}
                                </span>
                            </span>
                        </label>
                    ))}
                </div>
            </fieldset>

            <p className={styles.activeThemeStatus}>
                Aktive Darstellung:{' '}
                <strong>
                    {resolvedMode === 'dark' ? 'Dunkel' : 'Hell'}
                </strong>
                {mode === 'system'
                    ? ' entsprechend der Geräteeinstellung'
                    : ''}
            </p>
        </section>
    )
}