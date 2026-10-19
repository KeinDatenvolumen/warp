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