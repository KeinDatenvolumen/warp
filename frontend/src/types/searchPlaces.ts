// [KI-Log Nr. KI-08] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Geocoding-Client & Autocomplete (WARP-010)
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