// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Profilseite & Theme-System (WARP-033)
import {
    Navigate,
    Route,
    Routes,
} from 'react-router'
import ProtectedRoute from '../components/routing/ProtectedRoute'
import EmailVerificationPage from '../pages/EmailVerificationPage'
import ForgotPasswordPage from '../pages/ForgotPasswordPage'
import HomePage from '../pages/HomePage'
import LoginPage from '../pages/LoginPage'
import {NotFoundPage} from '../pages/NotFoundPage'
import ProfilePage from '../pages/ProfilePage'
import RegistrationPage from '../pages/RegistrationPage'
import ResetPasswordPage from '../pages/ResetPasswordPage'
import RoutePlannerPage from '../pages/RoutePlannerPage'
import SavedRouteDetailPage from '../pages/SavedRouteDetailPage'
import SavedRoutesPage from '../pages/SavedRoutesPage'
import {SystemStatusPage} from '../pages/SystemStatusPage'
import AppLayout from './AppLayout'

export default function AppRoutes() {
    return (
        <Routes>
            <Route element={<AppLayout />}>
                <Route
                    path="/"
                    element={
                        <Navigate
                            to="/route-planner"
                            replace
                        />
                    }
                />

                <Route
                    path="/login"
                    element={<LoginPage />}
                />

                <Route
                    path="/register"
                    element={<RegistrationPage />}
                />

                <Route
                    path="/verify-email"
                    element={<EmailVerificationPage />}
                />

                <Route
                    path="/forgot-password"
                    element={<ForgotPasswordPage />}
                />

                <Route
                    path="/reset-password"
                    element={<ResetPasswordPage />}
                />

                <Route
                    path="/route-planner"
                    element={<RoutePlannerPage />}
                />

                <Route
                    path="/profile"
                    element={
                        <ProtectedRoute>
                            <ProfilePage />
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/saved-routes"
                    element={
                        <ProtectedRoute>
                            <SavedRoutesPage />
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/saved-routes/:routeId"
                    element={
                        <ProtectedRoute>
                            <SavedRouteDetailPage />
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/system-health"
                    element={<HomePage />}
                />

                <Route
                    path="/system-status"
                    element={<SystemStatusPage />}
                />

                <Route
                    path="*"
                    element={<NotFoundPage />}
                />
            </Route>
        </Routes>
    )
}