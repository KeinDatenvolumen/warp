import {useQuery} from '@apollo/client/react'
import Loader from '../../components/ui/Loader/Loader'
import MessageCard from '../../components/ui/MessageCard/MessageCard'
import {ACTIVITY_PROFILES_QUERY} from '../../api/activityProfilesQuery'
import type {ActivityProfileCode} from '../../types/activityProfile'
import styles from './ActivityProfileSelector.module.css'

const SELECTED_PROFILE_STORAGE_KEY =
    'warp-selected-activity-profile'

interface ActivityProfileSelectorProps {
    selectedProfile: ActivityProfileCode | null
    onSelect: (profileCode: ActivityProfileCode) => void
}

export default function ActivityProfileSelector({
                                                    selectedProfile,
                                                    onSelect
                                                }: ActivityProfileSelectorProps) {
    const {data, loading, error} = useQuery(
        ACTIVITY_PROFILES_QUERY,
        {
            fetchPolicy: 'cache-first'
        }
    )

    if (loading) {
        return (
            <MessageCard>
                <Loader/>
                <p>Aktivitätsprofile werden geladen ...</p>
            </MessageCard>
        )
    }

    if (error) {
        return (
            <MessageCard error>
                <p>
                    Die Aktivitätsprofile konnten nicht geladen werden.
                </p>
            </MessageCard>
        )
    }

    const profiles = data?.activityProfiles ?? []

    function handleSelect(
        profileCode: ActivityProfileCode
    ): void {
        localStorage.setItem(
            SELECTED_PROFILE_STORAGE_KEY,
            profileCode
        )
        onSelect(profileCode)
    }

    return (
        <section
            className={styles.section}
            aria-labelledby="activity-profile-heading"
        >
            <div>
                <p className={styles.heading}>
                    Aktivitätsprofil
                </p>
                <h2 id="activity-profile-heading">
                    Wie möchtest du unterwegs sein?
                </h2>
            </div>

            {profiles.length === 0 && (
                <div className={styles.error}>
                    Es sind keine Aktivitätsprofile verfügbar.
                </div>
            )}

            <div className={styles.grid}>
                {profiles.map(profile => {
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
                                    : null
                            ]
                                .filter(Boolean)
                                .join(' ')}
                            aria-pressed={isSelected}
                            onClick={() => handleSelect(profile.code)}
                        >
                            <span className={styles.cardTitle}>
                                {profile.displayName}
                            </span>

                            <dl className={styles.details}>
                                <div className={styles.detail}>
                                    <dt className={styles.detailName}>
                                        Wetterabfrage
                                    </dt>
                                    <dd className={styles.detailValue}>
                                        alle{' '}
                                        {profile.weatherQueryIntervalMinutes}{' '}
                                        Minuten
                                    </dd>
                                </div>
                            </dl>
                        </button>
                    )
                })}
            </div>
        </section>
    )
}