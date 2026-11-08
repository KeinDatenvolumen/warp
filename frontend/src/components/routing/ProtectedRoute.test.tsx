// [KI-Log Nr. KI-126] (8495425) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: manage session status centrally and implement login form (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
/**
 * @vitest-environment jsdom
 */

import {render, screen} from '@testing-library/react'
import {MemoryRouter, Route, Routes, useLocation} from 'react-router'
import {describe, expect, it, vi} from 'vitest'
import {
    AuthContext,
    type AuthContextValue,
    type AuthStatus
} from '../../features/auth/AuthContext'
import ProtectedRoute from './ProtectedRoute'

function authValue(status: AuthStatus): AuthContextValue {
    return {
        status,
        session: null,
        isLoading: status === 'loading',
        isAuthenticated: status === 'authenticated',
        login: vi.fn(),
        logout: vi.fn()
    }
}

function LoginProbe() {
    const location = useLocation()
    const from = (location.state as { from?: { pathname: string } } | null)
        ?.from?.pathname

    return <p>Anmeldeseite (von {from ?? 'unbekannt'})</p>
}

function renderRoute(status: AuthStatus) {
    return render(
        <AuthContext.Provider value={authValue(status)}>
            <MemoryRouter initialEntries={['/profile']}>
                <Routes>
                    <Route path="/login" element={<LoginProbe/>}/>
                    <Route
                        path="/profile"
                        element={
                            <ProtectedRoute>
                                <p>Geheimer Inhalt</p>
                            </ProtectedRoute>
                        }
                    />
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>
    )
}

describe('ProtectedRoute', () => {
    it('zeigt während der Sitzungsprüfung keinen geschützten Inhalt', () => {
        renderRoute('loading')

        expect(screen.queryByText('Geheimer Inhalt')).not.toBeInTheDocument()
        expect(screen.getByRole('status')).toHaveTextContent(
            'Sitzung wird geprüft'
        )
    })

    it('leitet während der Sitzungsprüfung nicht zur Anmeldung um', () => {
        renderRoute('loading')

        expect(screen.queryByText(/Anmeldeseite/)).not.toBeInTheDocument()
    })

    it('leitet nicht angemeldete Benutzer zur Anmeldung um und merkt die Zielseite', () => {
        renderRoute('unauthenticated')

        expect(screen.queryByText('Geheimer Inhalt')).not.toBeInTheDocument()
        expect(
            screen.getByText('Anmeldeseite (von /profile)')
        ).toBeInTheDocument()
    })

    it('zeigt angemeldeten Benutzern den Inhalt', () => {
        renderRoute('authenticated')

        expect(screen.getByText('Geheimer Inhalt')).toBeInTheDocument()
    })
})