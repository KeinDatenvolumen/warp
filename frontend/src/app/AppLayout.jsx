import { Outlet } from 'react-router-dom'

export default function AppLayout() {
    return (
        <div className="app-shell">
            <main className="content-shell">
                <Outlet />
            </main>
        </div>
    )
}