// [KI-Log Nr. KI-032] (783455d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add application pages and routing (WARP-004)
// [KI-Log Nr. KI-037] (6fdd913) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: added authentication state and protected routes (WARP-004)
// [KI-Log Nr. KI-039] (b0345ea) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add application pages and routing (WARP-004)
// [KI-Log Nr. KI-041] (95411c2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: rebase to live (WARP-004)
// [KI-Log Nr. KI-044] (d75389d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: clean up duplicate layout and routes and corrected capital letter (WARP-004)
// [KI-Log Nr. KI-049] (358799f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: modular approach to styling (WARP-004)
// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-087] (486dab2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration page and route (WARP-005)
// [KI-Log Nr. KI-109] (de61707) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add registration page and route (WARP-005)
// [KI-Log Nr. KI-167] (af1ca24) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: add frontend email verification (WARP-007)
// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-210] (ce30513) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-031): responsives Theme-Design für das Frontend umsetzen
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
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
                        <ProtectedRoute requireVerified>
                            <SavedRoutesPage />
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/saved-routes/:routeId"
                    element={
                        <ProtectedRoute requireVerified>
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