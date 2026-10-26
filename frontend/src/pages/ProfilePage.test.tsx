/**
 * @vitest-environment jsdom
 */

import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {ApiRequestError} from '../api/restClient'
import ProfilePage from './ProfilePage'

const useQueryMock = vi.hoisted(() => vi.fn())
const changeEmailMock = vi.hoisted(() => vi.fn())
const changePasswordMock = vi.hoisted(() => vi.fn())

vi.mock('@apollo/client/react', () => ({
    useQuery: useQueryMock
}))

vi.mock('../api/accountApi', () => ({
    changeEmail: changeEmailMock,
    changePassword: changePasswordMock
}))

const user = {
    id: 'usr_1',
    name: 'Max Mustermann',
    email: 'max@example.de',
    emailVerified: true,
    createdAt: '2026-10-03T10:30:00Z'
}

function mockQuery(currentUser: typeof user | null, refetch = vi.fn()) {
    useQueryMock.mockReturnValue({
        data: {currentUser},
        loading: false,
        error: undefined,
        refetch
    })

    return refetch
}

describe('ProfilePage', () => {
    beforeEach(() => {
        changeEmailMock.mockReset()
        changeEmailMock.mockResolvedValue(undefined)
        changePasswordMock.mockReset()
        changePasswordMock.mockResolvedValue(undefined)
    })

    it('zeigt Name und E-Mail an, der Name ist nicht bearbeitbar', () => {
        mockQuery(user)

        render(<ProfilePage/>)

        expect(screen.getByText('Max Mustermann')).toBeInTheDocument()
        expect(screen.getByText('max@example.de')).toBeInTheDocument()
        expect(screen.getByText('Bestätigt')).toBeInTheDocument()
        expect(screen.queryByLabelText('Name')).not.toBeInTheDocument()
    })

    it('weist auf eine nicht bestätigte E-Mail-Adresse hin', () => {
        mockQuery({...user, emailVerified: false})

        render(<ProfilePage/>)

        expect(screen.getByText('Nicht bestätigt')).toBeInTheDocument()
        expect(
            screen.getByText(/noch nicht bestätigt/)
        ).toBeInTheDocument()
    })

    it('ändert die E-Mail-Adresse und lädt das Profil neu', async () => {
        const refetch = mockQuery(user)
        const userEventSession = userEvent.setup()

        render(<ProfilePage/>)

        await userEventSession.type(
            screen.getByLabelText('Neue E-Mail-Adresse'),
            'neu@example.de'
        )
        await userEventSession.type(
            screen.getByLabelText('Passwort zur Bestätigung'),
            'geheimes-passwort'
        )
        await userEventSession.click(
            screen.getByRole('button', {name: 'E-Mail-Adresse ändern'})
        )

        expect(changeEmailMock).toHaveBeenCalledWith({
            newEmail: 'neu@example.de',
            password: 'geheimes-passwort'
        })
        expect(refetch).toHaveBeenCalled()
        expect(screen.getByRole('status')).toHaveTextContent(
            'Bestätigungs-E-Mail'
        )
    })

    it('sendet nichts, wenn die Passwort-Wiederholung abweicht', async () => {
        mockQuery(user)
        const userEventSession = userEvent.setup()

        render(<ProfilePage/>)

        await userEventSession.type(
            screen.getByLabelText('Aktuelles Passwort'),
            'altes-passwort-123'
        )
        await userEventSession.type(
            screen.getByLabelText('Neues Passwort'),
            'neues-passwort-123'
        )
        await userEventSession.type(
            screen.getByLabelText('Neues Passwort wiederholen'),
            'abweichend-12345'
        )
        await userEventSession.click(
            screen.getByRole('button', {name: 'Passwort ändern'})
        )

        expect(
            screen.getByText('Die eingegebenen Passwörter stimmen nicht überein.')
        ).toBeInTheDocument()
        expect(changePasswordMock).not.toHaveBeenCalled()
    })

    it('zeigt bei falschem aktuellem Passwort einen Feldfehler', async () => {
        mockQuery(user)
        changePasswordMock.mockRejectedValue(
            new ApiRequestError(403, {code: 'INVALID_PASSWORD'})
        )
        const userEventSession = userEvent.setup()

        render(<ProfilePage/>)

        await userEventSession.type(
            screen.getByLabelText('Aktuelles Passwort'),
            'falsches-passwort-1'
        )
        await userEventSession.type(
            screen.getByLabelText('Neues Passwort'),
            'neues-passwort-123'
        )
        await userEventSession.type(
            screen.getByLabelText('Neues Passwort wiederholen'),
            'neues-passwort-123'
        )
        await userEventSession.click(
            screen.getByRole('button', {name: 'Passwort ändern'})
        )

        expect(
            await screen.findByText('Das aktuelle Passwort ist nicht korrekt.')
        ).toBeInTheDocument()
    })
})