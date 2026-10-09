import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './AppLayout.jsx'
import HomePage from '../pages/HomePage.jsx'
import LoginPage from '../pages/LoginPage.jsx'
import ProfilePage from '../pages/ProfilePage.jsx'
import RoutePlannerPage from '../pages/RoutePlannerPage.jsx'
import SavedRoutesPage from '../pages/SavedRoutesPage.jsx'
import ProtectedRoute from '../components/routing/ProtectedRoute.jsx'

export default function AppRoutes() {
    return (
        <Routes>
            <Route element={<AppLayout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/login" element={<LoginPage />} />
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
                    path="*"
                    element={<Navigate to="/" replace />}
                />
            </Route>
        </Routes>
    )
}