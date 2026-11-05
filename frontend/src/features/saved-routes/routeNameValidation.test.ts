// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
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