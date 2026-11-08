// [KI-Log Nr. KI-059] (2c55674) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add typescript to frontend, migration from javascript to typescript. update of documentation (WARP-001)
// [KI-Log Nr. KI-101] (c1e1a5c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: implement MapTiler place geocoding (WARP-010)
// [KI-Log Nr. KI-129] (d52e1bc) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve handling of first search and set min length to 3 in the routing service (WARP-010)
// [KI-Log Nr. KI-133] (943d006) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: implement MapTiler place geocoding (WARP-010)
// [KI-Log Nr. KI-139] (7de7075) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve handling of first search and set min length to 3 in the routing service (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_BFF_BASE_URL?: string
    readonly VITE_MAPTILER_PUBLIC_API_KEY?: string
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}