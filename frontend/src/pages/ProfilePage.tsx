import {useQuery} from '@apollo/client/react'
import {CURRENT_USER_QUERY} from '../api/currentUserQuery'
import Loader from '../components/ui/Loader/Loader'
import MessageCard from '../components/ui/MessageCard/MessageCard'
import ChangeEmailForm from '../features/profile/ChangeEmailForm'
import ChangePasswordForm from '../features/profile/ChangePasswordForm'
import profileStyles from '../features/profile/Profile.module.css'
import {formatDate} from '../features/saved-routes/format'
import styles from './PagePlaceholder.module.css'

export default function ProfilePage() {
    const {data, loading, error, refetch} = useQuery(CURRENT_USER_QUERY, {
        fetchPolicy: 'cache-and-network'
    })

    const user = data?.currentUser ?? null

    return (
        <main className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>Benutzerkonto</p>
                <h1>Profil</h1>
                <p className={styles.description}>
                    Hier siehst du deine Kontodaten und kannst deine
                    E-Mail-Adresse und dein Passwort ändern.
                </p>
            </section>

            <div className={profileStyles.sections}>
                {loading && !user && (
                    <MessageCard>
                        <Loader/>
                        <p role="status">Profil wird geladen …</p>
                    </MessageCard>
                )}

                {error && !user && (
                    <MessageCard error>
                        <div>
                            <p>Das Profil konnte nicht geladen werden.</p>
                            <button
                                type="button"
                                className={profileStyles.button}
                                onClick={() => void refetch()}
                            >
                                Erneut versuchen
                            </button>
                        </div>
                    </MessageCard>
                )}

                {user && (
                    <>
                        <section
                            className={profileStyles.section}
                            aria-labelledby="account-data-title"
                        >
                            <h2 id="account-data-title">Kontodaten</h2>

                            <dl className={profileStyles.infoList}>
                                <div>
                                    <dt>Name</dt>
                                    <dd>{user.name}</dd>
                                </div>
                                <div>
                                    <dt>E-Mail-Adresse</dt>
                                    <dd>{user.email}</dd>
                                </div>
                                <div>
                                    <dt>Status der E-Mail-Adresse</dt>
                                    <dd>
                                        <span
                                            className={
                                                user.emailVerified
                                                    ? `${profileStyles.badge} ${profileStyles.badgeSuccess}`
                                                    : `${profileStyles.badge} ${profileStyles.badgeWarning}`
                                            }
                                        >
                                            {user.emailVerified
                                                ? 'Bestätigt'
                                                : 'Nicht bestätigt'}
                                        </span>
                                    </dd>
                                </div>
                                <div>
                                    <dt>Registriert am</dt>
                                    <dd>{formatDate(user.createdAt)}</dd>
                                </div>
                            </dl>

                            <p className={profileStyles.sectionDescription}>
                                Der Name kann nicht geändert werden.
                            </p>

                            {!user.emailVerified && (
                                <p className={profileStyles.notice} role="status">
                                    Deine E-Mail-Adresse ist noch nicht
                                    bestätigt. Routen können erst nach der
                                    Bestätigung berechnet und gespeichert
                                    werden.
                                </p>
                            )}
                        </section>

                        <ChangeEmailForm
                            currentEmail={user.email}
                            onChanged={() => void refetch()}
                        />

                        <ChangePasswordForm/>
                    </>
                )}
            </div>
        </main>
    )
}