// [KI-Log Nr. KI-08] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Geocoding-Client & Autocomplete (WARP-010)
export function isLatestRequest(
    requestId: number,
    currentRequestId: number,
): boolean {
    return requestId === currentRequestId
}