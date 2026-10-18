export class SessionNotEstablishedError extends Error {
    constructor() {
        super('Die Sitzung konnte nach der Anmeldung nicht bestätigt werden.')
        this.name = 'SessionNotEstablishedError'
    }
}