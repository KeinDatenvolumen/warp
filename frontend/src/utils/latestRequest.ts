export function isLatestRequest(
    requestId: number,
    currentRequestId: number,
): boolean {
    return requestId === currentRequestId
}