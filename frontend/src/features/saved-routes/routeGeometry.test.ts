// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
import {describe, expect, it} from 'vitest'
import {
    buildRouteMapSegments,
    DEFAULT_ROUTE_COLOR,
    toLatLngPositions
} from './routeGeometry'

const line = (coordinates: number[][]) => ({type: 'LineString', coordinates})

describe('toLatLngPositions', () => {
    it('vertauscht Länge und Breite', () => {
        expect(toLatLngPositions([[9.15, 49.35]])).toEqual([[49.35, 9.15]])
    })

    it('ignoriert unvollständige Positionen', () => {
        expect(toLatLngPositions([[9.15], [9.2, 49.4]])).toEqual([[49.4, 9.2]])
    })
})

describe('buildRouteMapSegments', () => {
    it('nutzt farbige Abschnitte sortiert nach Index', () => {
        const segments = buildRouteMapSegments({
            geometry: line([]),
            weatherSegments: [
                {
                    segmentIndex: 1,
                    score: 40,
                    color: '#ff0000',
                    geometry: line([[9.3, 49.4], [9.4, 49.5]])
                },
                {
                    segmentIndex: 0,
                    score: 90,
                    color: '#00ff00',
                    geometry: line([[9.1, 49.3], [9.3, 49.4]])
                }
            ]
        })

        expect(segments.map((segment) => segment.color)).toEqual([
            '#00ff00',
            '#ff0000'
        ])
    })

    it('fällt auf die Gesamtgeometrie zurück', () => {
        const segments = buildRouteMapSegments({
            geometry: line([[9.1, 49.3], [9.7, 49.5]]),
            weatherSegments: []
        })

        expect(segments).toEqual([
            {
                positions: [[49.3, 9.1], [49.5, 9.7]],
                color: DEFAULT_ROUTE_COLOR
            }
        ])
    })

    it('liefert bei leerer Geometrie keine Linie', () => {
        expect(
            buildRouteMapSegments({geometry: line([]), weatherSegments: []})
        ).toEqual([])
    })
})