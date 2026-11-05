// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
export class SessionNotEstablishedError extends Error {
    constructor() {
        super('Die Sitzung konnte nach der Anmeldung nicht bestätigt werden.')
        this.name = 'SessionNotEstablishedError'
    }
}