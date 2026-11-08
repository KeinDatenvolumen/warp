// [KI-Log Nr. KI-198] (884c88d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add favorite API operations and toggle button (WARP-022)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
/**
 * @vitest-environment jsdom
 */

import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import FavoriteButton from './FavoriteButton'

const useMutationMock = vi.hoisted(() => vi.fn())

vi.mock('@apollo/client/react', () => ({
    useMutation: useMutationMock
}))

describe('FavoriteButton', () => {
    const updateMock = vi.fn()

    beforeEach(() => {
        updateMock.mockReset()
        updateMock.mockResolvedValue({
            data: {updateSavedRoute: {route: {id: 'route_1'}, errors: []}}
        })
        useMutationMock.mockReturnValue([updateMock, {loading: false}])
    })

    it('markiert eine Route als Favorit', async () => {
        const onChanged = vi.fn()
        const user = userEvent.setup()

        render(
            <FavoriteButton
                routeId="route_1"
                routeName="Fahrradtour"
                favorite={false}
                onChanged={onChanged}
            />
        )

        const button = screen.getByRole('button', {
            name: 'Route Fahrradtour als Favorit markieren'
        })

        expect(button).toHaveAttribute('aria-pressed', 'false')

        await user.click(button)

        expect(updateMock).toHaveBeenCalledWith({
            variables: {id: 'route_1', input: {favorite: true}}
        })
        expect(onChanged).toHaveBeenCalled()
    })

    it('entfernt die Favoritenmarkierung', async () => {
        const user = userEvent.setup()

        render(
            <FavoriteButton
                routeId="route_1"
                routeName="Fahrradtour"
                favorite
            />
        )

        const button = screen.getByRole('button', {
            name: 'Route Fahrradtour als Favorit markieren'
        })

        expect(button).toHaveAttribute('aria-pressed', 'true')

        await user.click(button)

        expect(updateMock).toHaveBeenCalledWith({
            variables: {id: 'route_1', input: {favorite: false}}
        })
    })

    it('meldet einen Fehler und lädt nicht neu', async () => {
        updateMock.mockRejectedValue(new Error('boom'))
        const onError = vi.fn()
        const onChanged = vi.fn()
        const user = userEvent.setup()

        render(
            <FavoriteButton
                routeId="route_1"
                routeName="Fahrradtour"
                favorite={false}
                onError={onError}
                onChanged={onChanged}
            />
        )

        await user.click(screen.getByRole('button'))

        expect(onError).toHaveBeenLastCalledWith(
            expect.stringContaining('konnte nicht geändert werden')
        )
        expect(onChanged).not.toHaveBeenCalled()
    })
})