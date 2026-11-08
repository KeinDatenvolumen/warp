// [KI-Log Nr. KI-208] (c29d799) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add GPX route export (WARP-032)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import {
    canExportRouteAsGpx,
    createGpxDocument,
    createGpxFileName,
} from './gpxExport'

describe('gpxExport', () => {
    it('creates a GPX document from GeoJSON coordinates', () => {
        const result = createGpxDocument({
            startLabel: 'Mosbach',
            destinationLabel: 'Bad Mergentheim',
            geometry: {
                type: 'LineString',
                coordinates: [
                    [9.1511, 49.3536],
                    [9.7736, 49.4924],
                ],
            },
        })

        expect(result).toContain(
            '<name>Mosbach nach Bad Mergentheim</name>',
        )

        expect(result).toContain(
            '<trkpt lat="49.3536" lon="9.1511" />',
        )

        expect(result).toContain(
            '<trkpt lat="49.4924" lon="9.7736" />',
        )
    })

    it('escapes XML characters in route names', () => {
        const result = createGpxDocument({
            startLabel: 'Start & <Ort>',
            destinationLabel: 'Ziel "Nord"',
            geometry: {
                type: 'LineString',
                coordinates: [
                    [9.1, 49.3],
                    [9.2, 49.4],
                ],
            },
        })

        expect(result).toContain(
            'Start &amp; &lt;Ort&gt; nach Ziel &quot;Nord&quot;',
        )
    })

    it('rejects a route without enough valid coordinates', () => {
        expect(() =>
            createGpxDocument({
                startLabel: 'Mosbach',
                destinationLabel: 'Heilbronn',
                geometry: {
                    type: 'LineString',
                    coordinates: [[9.1, 49.3]],
                },
            }),
        ).toThrow(
            'Die Route enthält keine exportierbare Geometrie.',
        )
    })

    it('recognizes exportable route geometry', () => {
        expect(
            canExportRouteAsGpx({
                type: 'LineString',
                coordinates: [
                    [9.1, 49.3],
                    [9.2, 49.4],
                ],
            }),
        ).toBe(true)
    })

    it('rejects another geometry type', () => {
        expect(
            canExportRouteAsGpx({
                type: 'Point',
                coordinates: [
                    [9.1, 49.3],
                    [9.2, 49.4],
                ],
            }),
        ).toBe(false)
    })

    it('creates a safe GPX file name', () => {
        expect(
            createGpxFileName(
                'Möckmühl Zentrum',
                'Bad Mergentheim',
            ),
        ).toBe(
            'mockmuhl-zentrum-nach-bad-mergentheim.gpx',
        )
    })

    it('uses a fallback for an empty file name', () => {
        expect(
            createGpxFileName('---', '!!!'),
        ).toBe('warp-route.gpx')
    })

    it('uses short place names for the GPX file name', () => {
        expect(
            createGpxFileName(
                'Schillingstadt, Ahorn, Deutschland',
                'Schwabhausen, Boxberg, Deutschland',
            ),
        ).toBe(
            'schillingstadt-nach-schwabhausen.gpx',
        )
    })

})