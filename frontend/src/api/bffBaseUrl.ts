// [KI-Log Nr. KI-120] (61da337) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add session API with CSRF handling and Retry-After support (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export const BFF_BASE_URL = (
    import.meta.env.VITE_BFF_BASE_URL ?? ''
).replace(/\/$/, '')