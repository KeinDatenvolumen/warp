import {Route, Routes} from 'react-router'
import AppLayout from './AppLayout'
import HomePage from '../pages/HomePage'
import LoginPage from '../pages/LoginPage'
import ProfilePage from '../pages/ProfilePage'
import RoutePlannerPage from '../pages/RoutePlannerPage'
import SavedRoutesPage from '../pages/SavedRoutesPage'
import SavedRouteDetailPage from '../pages/SavedRouteDetailPage'
import {SystemStatusPage} from "../pages/SystemStatusPage";
import {NotFoundPage} from "../pages/NotFoundPage";
import ProtectedRoute from '../components/routing/ProtectedRoute'
import RegistrationPage from '../pages/RegistrationPage'
import EmailVerificationPage from '../pages/EmailVerificationPage'

export default function AppRoutes() {
    return (
        <Routes>
            <Route element={<AppLayout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegistrationPage />} />
                <Route path="/verify-email" element={<EmailVerificationPage />} />
                <Route path="/route-planner" element={<RoutePlannerPage />} />
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
                <Route path="/system-status" element={<SystemStatusPage />} />
                <Route path="*" element={<NotFoundPage />} />
                <Route
                    path="/saved-routes/:routeId"
                    element={
                        <ProtectedRoute>
                            <SavedRouteDetailPage />
                        </ProtectedRoute>
                    }
                />
            </Route>
        </Routes>
    )
}