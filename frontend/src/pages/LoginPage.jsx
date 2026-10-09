import {Navigate, useLocation, useNavigate} from 'react-router'
import {useAuth} from '../features/auth/useAuth.js'
import styles from './LoginPage.module.css'

export default function LoginPage() {
    const {isAuthenticated, login} = useAuth();
    const location = useLocation();
    const navigate = useNavigate();
    const targetPath = location.state?.from?.pathname ?? '/'
    if (isAuthenticated) return <Navigate to={targetPath} replace/>

    function handleSubmit(event) {
        event.preventDefault();
        login();
        navigate(targetPath, {replace: true})
    }

    return <main className={styles.page}>
        <section className={styles.card}><p className={styles.eyebrow}>Benutzerkonto</p><h1>Anmelden</h1><p
            className={styles.description}>Die echte Anmeldung wird später mit dem Identity-Service verbunden.</p>
            <form onSubmit={handleSubmit}>
                <button type="submit" className={styles.button}>Testweise anmelden</button>
            </form>
        </section>
    </main>
}
