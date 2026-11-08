// [KI-Log Nr. KI-064] (f2841c9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add frontend activity profile API (WARP-033)
// [KI-Log Nr. KI-065] (73f94e9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add activity profile selection to route planner (WARP-033)
// [KI-Log Nr. KI-145] (821b60d) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: hide weather query interval from users and use km (hiking 2, bicycle 5, motorcycle 15) (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export type ActivityProfileCode =
    | 'BICYCLE'
    | 'MOTORCYCLE'
    | 'HIKING'

export interface ActivityProfile {
    code: ActivityProfileCode
    displayName: string
}