// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {describe, expect, it} from 'vitest'
import {MAX_ROUTE_NAME_LENGTH, validateRouteName} from './routeNameValidation'

describe('validateRouteName', () => {
    it('lehnt leere Namen und reine Leerzeichen ab', () => {
        expect(validateRouteName('')).toBeDefined()
        expect(validateRouteName('    ')).toBeDefined()
    })

    it('akzeptiert einen gültigen Namen', () => {
        expect(validateRouteName('Fahrradtour')).toBeUndefined()
    })

    it('begrenzt die Länge nach dem Trimmen', () => {
        const maximum = 'a'.repeat(MAX_ROUTE_NAME_LENGTH)

        expect(validateRouteName(` ${maximum} `)).toBeUndefined()
        expect(validateRouteName(`${maximum}a`)).toBeDefined()
    })
})