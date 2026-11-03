/**
 * @vitest-environment jsdom
 */
import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SystemStatusPage } from './SystemStatusPage'

const useQueryMock = vi.hoisted(() => vi.fn())

vi.mock('@apollo/client/react', () => ({
    useQuery: useQueryMock,
}))

describe('SystemStatusPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('zeigt Ladezustand an', () => {
        useQueryMock.mockReturnValue({
            data: null,
            loading: true,
            error: null,
            refetch: vi.fn(),
            networkStatus: 1,
        })

        render(<SystemStatusPage />)
        expect(screen.getByText('Systemstatus wird geladen')).toBeInTheDocument()
    })

    it('filtert Apollo-Minified-URLs und zeigt eine verständliche Fehlermeldung', () => {
        useQueryMock.mockReturnValue({
            data: null,
            loading: false,
            error: {
                message:
                    'An error occurred! For more details, see the full error text at https://go.apollo.dev/c/err#%7B%22version%22%3A%224.3.5%22%2C%22message%22%3A96%2C%22args%22%3A%5B%5D%7D',
            },
            refetch: vi.fn(),
            networkStatus: 8,
        })

        render(<SystemStatusPage />)
        expect(screen.getByText('Systemstatus nicht verfügbar')).toBeInTheDocument()
        expect(
            screen.getByText(
                'Der Systemstatus konnte momentan nicht geladen werden. Bitte prüfe deine Verbindung und versuche es erneut.'
            )
        ).toBeInTheDocument()
        expect(screen.queryByText(/go\.apollo\.dev/)).not.toBeInTheDocument()
    })

    it('zeigt übermittelte Backend-Fehlermeldungen verständlich an', () => {
        useQueryMock.mockReturnValue({
            data: null,
            loading: false,
            error: {
                graphQLErrors: [
                    { message: 'Wartungsarbeiten werden derzeit durchgeführt.' },
                ],
            },
            refetch: vi.fn(),
            networkStatus: 8,
        })

        render(<SystemStatusPage />)
        expect(
            screen.getByText('Wartungsarbeiten werden derzeit durchgeführt.')
        ).toBeInTheDocument()
    })
})
