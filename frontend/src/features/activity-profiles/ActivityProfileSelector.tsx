// [KI-Log Nr. KI-065] (73f94e9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add activity profile selection to route planner (WARP-033)
// [KI-Log Nr. KI-066] (78a6ad4) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve activity profile selection cards (WARP-033)
// [KI-Log Nr. KI-145] (821b60d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: hide weather query interval from users and use km (hiking 2, bicycle 5, motorcycle 15) (WARP-033)
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {useQuery} from '@apollo/client/react'
import {ACTIVITY_PROFILES_QUERY} from '../../api/activityProfilesQuery'
import Loader from '../../components/ui/Loader/Loader'
import MessageCard from '../../components/ui/MessageCard/MessageCard'
import type {ActivityProfileCode} from '../../types/activityProfile'
import styles from './ActivityProfileSelector.module.css'

const PROFILE_ICONS: Record<ActivityProfileCode, string> = {
    BICYCLE: '🚲',
    MOTORCYCLE: '🏍️',
    HIKING: '🥾',
}

const PROFILE_DESCRIPTIONS: Record<ActivityProfileCode, string> = {
    BICYCLE: 'Für alltägliche und sportliche Fahrradfahrten',
    MOTORCYCLE: 'Für wetterbewusste Touren mit dem Motorrad',
    HIKING: 'Für Wanderungen und längere Wege zu Fuß',
}

interface ActivityProfileSelectorProps {
    selectedProfile: ActivityProfileCode | null
    onSelect: (profileCode: ActivityProfileCode) => void
}

export default function ActivityProfileSelector({
                                                    selectedProfile,
                                                    onSelect,
                                                }: ActivityProfileSelectorProps) {
    const {
        data,
        loading,
        error,
        refetch,
    } = useQuery(ACTIVITY_PROFILES_QUERY, {
        fetchPolicy: 'cache-first',
    })

    if (loading) {
        return (
            <MessageCard>
                <Loader />

                <p role="status">
                    Aktivitätsprofile werden geladen …
                </p>
            </MessageCard>
        )
    }

    if (error) {
        return (
            <MessageCard error>
                <div className={styles.loadError}>
                    <div>
                        <h2>
                            Aktivitätsprofile nicht verfügbar
                        </h2>

                        <p>
                            Die Aktivitätsprofile konnten nicht geladen
                            werden. Prüfe die Verbindung und versuche es
                            erneut.
                        </p>
                    </div>

                    <button
                        type="button"
                        className={styles.retryButton}
                        onClick={() => {
                            void refetch()
                        }}
                    >
                        Erneut versuchen
                    </button>
                </div>
            </MessageCard>
        )
    }

    const profiles = data?.activityProfiles ?? []

    return (
        <section
            className={styles.section}
            aria-labelledby="activity-profile-heading"
        >
            <div className={styles.sectionHeading}>
                <div>
                    <p className={styles.heading}>
                        Aktivitätsprofil
                    </p>

                    <h2 id="activity-profile-heading">
                        Wie möchtest du unterwegs sein?
                    </h2>
                </div>

                {selectedProfile && (
                    <span
                        className={styles.selectionStatus}
                        role="status"
                    >
                        Profil ausgewählt
                    </span>
                )}
            </div>

            <p className={styles.introduction}>
                Das Aktivitätsprofil beeinflusst die Streckenführung
                und die Bewertung der Wetterbedingungen.
            </p>

            {profiles.length === 0 ? (
                <div
                    className={styles.emptyState}
                    role="status"
                >
                    <span
                        className={styles.emptyStateIcon}
                        aria-hidden="true"
                    >
                        !
                    </span>

                    <div>
                        <h3>
                            Keine Aktivitätsprofile verfügbar
                        </h3>

                        <p>
                            Der Server hat derzeit keine auswählbaren
                            Profile zurückgegeben.
                        </p>
                    </div>
                </div>
            ) : (
                <div
                    className={styles.grid}
                    role="group"
                    aria-label="Aktivitätsprofil auswählen"
                >
                    {profiles.map((profile) => {
                        const isSelected =
                            profile.code === selectedProfile

                        return (
                            <button
                                key={profile.code}
                                type="button"
                                className={[
                                    styles.card,
                                    isSelected
                                        ? styles.cardSelected
                                        : '',
                                ]
                                    .filter(Boolean)
                                    .join(' ')}
                                aria-label={`${profile.displayName}, ${PROFILE_DESCRIPTIONS[profile.code]}`}
                                aria-pressed={isSelected}
                                onClick={() => {
                                    onSelect(profile.code)
                                }}
                            >
                                <span className={styles.cardHeader}>
                                    <span
                                        className={styles.icon}
                                        aria-hidden="true"
                                    >
                                        <span
                                            className={styles.iconGlyph}
                                        >
                                            {
                                                PROFILE_ICONS[
                                                    profile.code
                                                    ]
                                            }
                                        </span>
                                    </span>

                                    <span
                                        className={styles.selectionMark}
                                        aria-hidden="true"
                                    >
                                        {isSelected ? '✓' : ''}
                                    </span>
                                </span>

                                <span className={styles.cardContent}>
                                    <span className={styles.cardTitle}>
                                        {profile.displayName}
                                    </span>

                                    <span
                                        className={styles.description}
                                    >
                                        {
                                            PROFILE_DESCRIPTIONS[
                                                profile.code
                                                ]
                                        }
                                    </span>
                                </span>

                                <span
                                    className={styles.cardAction}
                                    aria-hidden="true"
                                >
                                    {isSelected
                                        ? 'Ausgewählt'
                                        : 'Auswählen'}
                                </span>
                            </button>
                        )
                    })}
                </div>
            )}
        </section>
    )
}