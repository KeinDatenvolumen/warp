// [KI-Log Nr. KI-126] (8495425) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: manage session status centrally and implement login form (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export class SessionNotEstablishedError extends Error {
    constructor() {
        super('Die Sitzung konnte nach der Anmeldung nicht bestätigt werden.')
        this.name = 'SessionNotEstablishedError'
    }
}