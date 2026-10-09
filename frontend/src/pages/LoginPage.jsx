import {
    Navigate,
    useLocation,
    useNavigate
} from 'react-router'
import { useAuth } from '../features/auth/useAuth.js'

export default function LoginPage() {
    const {
        isAuthenticated,
        login
    } = useAuth()

    const location = useLocation()
    const navigate = useNavigate()

    const targetPath =
        location.state?.from?.pathname ?? '/'

    if (isAuthenticated) {
        return (
            <Navigate
                to={targetPath}
                replace
            />
        )
    }

    function handleSubmit(event) {
        event.preventDefault()
        login()
        navigate(targetPath, { replace: true })
    }

    return (
        <main className="page">
            <section className="welcome-card">
                <p className="eyebrow">
                    Benutzerkonto
                </p>

                <h1>Anmelden</h1>

                <p className="welcome-description">
                    Die echte Anmeldung wird später mit dem
                    Identity-Service verbunden.
                </p>

                <form onSubmit={handleSubmit}>
                    <button
                        type="submit"
                        className="primary-link"
                    >
                        Testweise anmelden
                    </button>
                </form>
            </section>
        </main>
    )
}