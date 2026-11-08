// [KI-Log Nr. KI-098] (f9cda8a) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add place search inputs and GraphQL query (WARP-010)
// [KI-Log Nr. KI-131] (a369159) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add place search inputs and GraphQL query (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export interface Place {
placeId: string
displayName: string
latitude: number
longitude: number
}

export interface SearchPlacesQueryData {
searchPlaces: Place[]
}

export interface SearchPlacesQueryVariables {
query: string
limit?: number
}