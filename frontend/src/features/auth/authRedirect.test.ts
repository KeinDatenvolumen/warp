// [KI-Log Nr. KI-125] (d8c8a50) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add login validation, error mapping, redirect target and countdown helper (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {describe, expect, it} from 'vitest'
import {
    DEFAULT_AUTHENTICATED_PATH,
    resolveRedirectTarget
} from './authRedirect'

describe('resolveRedirectTarget', () => {
    it('verwendet die ursprünglich angefragte geschützte Seite', () => {
        expect(
            resolveRedirectTarget({
                from: {pathname: '/saved-routes', search: '?page=2', hash: '#a'}
            })
        ).toBe('/saved-routes?page=2#a')
    })

    it('verwendet ohne State das Standardziel', () => {
        expect(resolveRedirectTarget(undefined)).toBe(
            DEFAULT_AUTHENTICATED_PATH
        )
        expect(resolveRedirectTarget(null)).toBe(
            DEFAULT_AUTHENTICATED_PATH
        )
        expect(resolveRedirectTarget({})).toBe(
            DEFAULT_AUTHENTICATED_PATH
        )
    })

    it.each([
        'https://evil.example',
        '//evil.example',
        'profile',
        '/login',
        '/register',
        42,
        undefined
    ])('lehnt das Ziel %s ab', (pathname) => {
        expect(
            resolveRedirectTarget({from: {pathname}})
        ).toBe(DEFAULT_AUTHENTICATED_PATH)
    })
})