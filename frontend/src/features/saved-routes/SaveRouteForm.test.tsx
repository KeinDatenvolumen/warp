/**
 * @vitest-environment jsdom
 */

import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {MemoryRouter} from 'react-router'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import SaveRouteForm from './SaveRouteForm'

const useMutationMock = vi.hoisted(() => vi.fn())

vi.mock('@apollo/client/react', () => ({
    useMutation: useMutationMock
}))

function renderForm() {
    return render(
        <MemoryRouter>
            <SaveRouteForm
                routeCalculationId="calc_1"
                weatherEvaluationId="weather_1"
            />
        </MemoryRouter>
    )
}

describe('SaveRouteForm', () => {
    const saveMock = vi.fn()

    beforeEach(() => {
        saveMock.mockReset()
        useMutationMock.mockReturnValue([saveMock, {loading: false}])
    })

    it('verlangt einen Namen und ruft dann nichts auf', async () => {
        const user = userEvent.setup()

        renderForm()

        await user.click(screen.getByRole('button', {name: 'Route speichern'}))

        expect(screen.getByRole('alert')).toHaveTextContent(
            'Bitte gib einen Namen'
        )
        expect(saveMock).not.toHaveBeenCalled()
    })

    it('speichert mit getrimmtem Namen und den Referenz-IDs', async () => {
        saveMock.mockResolvedValue({
            data: {
                saveRoute: {
                    route: {
                        id: 'route_1',
                        name: 'Wochenendtour',
                        favorite: false,
                        createdAt: '2026-10-21T10:00:00Z'
                    },
                    errors: []
                }
            }
        })
        const user = userEvent.setup()

        renderForm()

        await user.type(
            screen.getByLabelText('Name der Route'),
            '  Wochenendtour  '
        )
        await user.click(screen.getByRole('button', {name: 'Route speichern'}))

        expect(saveMock).toHaveBeenCalledWith(
            expect.objectContaining({
                variables: {
                    input: {
                        name: 'Wochenendtour',
                        routeCalculationId: 'calc_1',
                        weatherEvaluationId: 'weather_1',
                        favorite: false
                    }
                }
            })
        )
        expect(await screen.findByRole('status')).toHaveTextContent(
            'Wochenendtour'
        )
    })

    it('erklärt, dass die E-Mail-Adresse bestätigt werden muss', async () => {
        saveMock.mockRejectedValue({
            errors: [{extensions: {code: 'EMAIL_NOT_VERIFIED'}}]
        })
        const user = userEvent.setup()

        renderForm()

        await user.type(screen.getByLabelText('Name der Route'), 'Tour')
        await user.click(screen.getByRole('button', {name: 'Route speichern'}))

        expect(await screen.findByRole('alert')).toHaveTextContent(
            'E-Mail-Adresse'
        )
    })
})
