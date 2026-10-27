import type { RouteGeometry } from '../../types/routeCalculation'

export interface GpxExportRoute {
    startLabel: string
    destinationLabel: string
    geometry: RouteGeometry
}

function escapeXml(value: string): string {
    return value
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&apos;')
}

function isValidCoordinate(
    coordinate: [number, number],
): boolean {
    const [longitude, latitude] = coordinate

    return (
        Number.isFinite(longitude) &&
        Number.isFinite(latitude) &&
        longitude >= -180 &&
        longitude <= 180 &&
        latitude >= -90 &&
        latitude <= 90
    )
}

function getValidCoordinates(
    geometry: RouteGeometry,
): [number, number][] {
    if (
        geometry.type !== 'LineString' ||
        !Array.isArray(geometry.coordinates)
    ) {
        return []
    }

    return geometry.coordinates.filter(isValidCoordinate)
}

export function canExportRouteAsGpx(
    geometry: RouteGeometry | null | undefined,
): boolean {
    return (
        geometry !== null &&
        geometry !== undefined &&
        getValidCoordinates(geometry).length >= 2
    )
}

export function createGpxDocument(
    route: GpxExportRoute,
): string {
    const coordinates = getValidCoordinates(route.geometry)

    if (coordinates.length < 2) {
        throw new Error(
            'Die Route enthält keine exportierbare Geometrie.',
        )
    }

    const routeName =
        `${route.startLabel} nach ${route.destinationLabel}`

    const trackPoints = coordinates
        .map(([longitude, latitude]) => {
            return `      <trkpt lat="${latitude}" lon="${longitude}" />`
        })
        .join('\n')

    return [
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<gpx',
        '  version="1.1"',
        '  creator="WaRP"',
        '  xmlns="http://www.topografix.com/GPX/1/1"',
        '  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"',
        '  xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd"',
        '>',
        '  <metadata>',
        `    <name>${escapeXml(routeName)}</name>`,
        '  </metadata>',
        '  <trk>',
        `    <name>${escapeXml(routeName)}</name>`,
        '    <trkseg>',
        trackPoints,
        '    </trkseg>',
        '  </trk>',
        '</gpx>',
        '',
    ].join('\n')
}

export function createGpxFileName(
    startLabel: string,
    destinationLabel: string,
): string {
    const getShortPlaceName = (label: string): string =>
        label.split(',')[0]?.trim() ?? ''

    const sanitizeLabel = (label: string): string =>
        label
            .normalize('NFKD')
            .replace(/[\u0300-\u036f]/g, '')
            .toLowerCase()
            .replace(/ß/g, 'ss')
            .replace(/[^a-z0-9]+/g, '-')
            .replace(/^-+|-+$/g, '')

    const sanitizedStart = sanitizeLabel(
        getShortPlaceName(startLabel),
    )
    const sanitizedDestination = sanitizeLabel(
        getShortPlaceName(destinationLabel),
    )

    if (!sanitizedStart || !sanitizedDestination) {
        return 'warp-route.gpx'
    }

    const baseName =
        `${sanitizedStart}-nach-${sanitizedDestination}`
            .slice(0, 100)
            .replace(/-+$/g, '')

    return `${baseName}.gpx`
}

export function downloadRouteAsGpx(
    route: GpxExportRoute,
): void {
    const gpxDocument = createGpxDocument(route)
    const fileName = createGpxFileName(
        route.startLabel,
        route.destinationLabel,
    )

    const blob = new Blob(
        [gpxDocument],
        {
            type: 'application/gpx+xml;charset=utf-8',
        },
    )

    const objectUrl = URL.createObjectURL(blob)
    const downloadLink = document.createElement('a')

    downloadLink.href = objectUrl
    downloadLink.download = fileName
    downloadLink.style.display = 'none'

    document.body.appendChild(downloadLink)

    try {
        downloadLink.click()
    } finally {
        downloadLink.remove()
        URL.revokeObjectURL(objectUrl)
    }
}