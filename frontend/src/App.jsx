import {
    Navigate,
    Route,
    Routes,
} from 'react-router'
import { AppLayout } from './components/AppLayout.jsx'
import { HomePage } from './pages/HomePage.jsx'
import { NotFoundPage } from './pages/NotFoundPage.jsx'
import { SystemStatusPage } from './pages/SystemStatusPage.jsx'

function App() {
    return (
        <Routes>
            <Route element={<AppLayout />}>
                <Route
                    index
                    element={<HomePage />}
                />

                <Route
                    path="system-status"
                    element={<SystemStatusPage />}
                />

                <Route
                    path="status"
                    element={
                        <Navigate
                            to="/system-status"
                            replace
                        />
                    }
                />

                <Route
                    path="*"
                    element={<NotFoundPage />}
                />
                </Route>
        </Routes>
    )
}

export default App