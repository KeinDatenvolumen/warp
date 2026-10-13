import {useState} from 'react'
import RouteMap from '../components/RouteMap/RouteMap'
import ActivityProfileSelector from '../features/activity-profiles/ActivityProfileSelector'
import type {ActivityProfileCode} from '../types/activityProfile'
import styles from './PagePlaceholder.module.css'

const SELECTED_PROFILE_STORAGE_KEY =
    'warp-selected-activity-profile'

function readSelectedProfile(): ActivityProfileCode | null {
    const value = localStorage.getItem(
        SELECTED_PROFILE_STORAGE_KEY
    )

    if (
        value === 'BICYCLE' ||
        value === 'MOTORCYCLE' ||
        value === 'HIKING'
    ) {
        return value
    }

    return null
}

export default function RoutePlannerPage() {
    const [selectedProfile, setSelectedProfile] =
        useState<ActivityProfileCode | null>(
            readSelectedProfile
        )

    return (
        <main className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>
                    Routenplanung
                </p>

                <h1>Routenplaner</h1>

                <p className={styles.description}>
                    Wähle ein Aktivitätsprofil für deine zukünftige
                    wetterabhängige Routenplanung.
                </p>
            </section>

            <div style={{marginTop: '1.5rem'}}>
                <ActivityProfileSelector
                    selectedProfile={selectedProfile}
                    onSelect={setSelectedProfile}
                />
            </div>

            <div style={{marginTop: '1.5rem'}}>
                <RouteMap/>
            </div>
        </main>
    )
}