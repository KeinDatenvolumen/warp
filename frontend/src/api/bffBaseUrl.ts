// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
export const BFF_BASE_URL = (
    import.meta.env.VITE_BFF_BASE_URL ?? ''
).replace(/\/$/, '')