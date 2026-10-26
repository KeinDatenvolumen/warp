import {
    describe,
    expect,
    it,
} from 'vitest'
import { toLeafletRoutePositions } from './routeGeometry'

describe('toLeafletRoutePositions', () => {
    it('converts GeoJSON coordinates to Leaflet positions', () => {
        const result = toLeafletRoutePositions({
            type: 'LineString',
            coordinates: [
                [9.15, 49.35],
                [9.2, 49.4],
            ],
        })

        expect(result).toEqual([
            [49.35, 9.15],
            [49.4, 9.2],
        ])
    })

    it('returns no positions for another geometry type', () => {
        const result = toLeafletRoutePositions({
            type: 'Point',
            coordinates: [[9.15, 49.35]],
        })

        expect(result).toEqual([])
    })

    it('filters invalid coordinates', () => {
        const result = toLeafletRoutePositions({
            type: 'LineString',
            coordinates: [
                [9.15, 49.35],
                [200, 49.4],
                [9.2, 95],
            ],
        })

        expect(result).toEqual([
            [49.35, 9.15],
        ])
    })

    it('returns no positions without geometry', () => {
        expect(toLeafletRoutePositions(null)).toEqual([])
    })
})