import type {FormEvent} from 'react'
import {
    Link,
    Navigate,
    useLocation,
    useNavigate,
    type Location
} from 'react-router'
import {useAuth} from '../features/auth/useAuth'
import styles from './LoginPage.module.css'

interface LoginLocationState {
    from?: Location
}

export default function LoginPage() {
    const {isAuthenticated, login} = useAuth()
    const location = useLocation() as Location<LoginLocationState>
    const navigate = useNavigate()

    const targetPath = location.state?.from?.pathname ?? '/'

    if (isAuthenticated) {
        return <Navigate to={targetPath} replace />
    }

    function handleSubmit(
        event: FormEvent<HTMLFormElement>
    ): void {
        event.preventDefault()
        login()
        navigate(targetPath, {replace: true})
    }

    return (
        <main className={styles.page}>
            <section className={styles.card}>
                <p className={styles.eyebrow}>Benutzerkonto</p>
                <h1>Anmelden</h1>

                <p className={styles.description}>
                    Die echte Anmeldung wird später mit dem
                    Identity-Service verbunden.
                </p>

                <form onSubmit={handleSubmit}>
                    <button type="submit" className={styles.button}>
                        Testweise anmelden
                    </button>
                </form>

                <p className={styles.registrationPrompt}>
                    Noch keinen Account?{' '}
                    <Link to="/register">
                        Registrieren
                    </Link>
                </p>
            </section>
        </main>
    )
}