/**
 * @vitest-environment jsdom
 */

import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import AiWeatherAdvisorPanel from './AiWeatherAdvisorPanel'
import type { RouteWeatherAiAnalysisResponse } from '../../types/weatherAiAnalysis'

const sampleAnalysis: RouteWeatherAiAnalysisResponse = {
    routeCalculationId: 'calc_123',
    activityProfileCode: 'MOTORCYCLE',
    activityProfileDisplayName: 'Motorrad',
    departureTime: '2026-05-16T08:00:00Z',
    overallScore: 68,
    thunderstormCapApplied: false,
    riskLevel: 'HIGH',
    headline: 'Wechselhafte Bedingungen mit Seitenwind im mittleren Abschnitt',
    overallSummary:
        'Auf deiner Motorrad-Route treten ab Kilometer 10 kräftige Böenspitzen in Kombination mit einer noch feuchten Straßenoberfläche auf.',
    activityProfileAdvice: '',
    hiddenParameterInsights: [],
    criticalSections: [],
    recommendations: [
        'Vor Fahrtantritt Visier-Belüftung öffnen.',
        'Im Abschnitt km 10–20 vorsichtig bremsen.',
    ],
    mcpToolsUsed: [],
    modelName: 'gemini-3.5-flash',
    generatedAt: '2026-05-16T07:55:00Z',
}

describe('AiWeatherAdvisorPanel', () => {
    it('zeigt einen Login-Hinweis für nicht angemeldete Nutzer', () => {
        render(
            <MemoryRouter>
                <AiWeatherAdvisorPanel
                    analysis={null}
                    loading={false}
                    error={null}
                    isAuthenticated={false}
                    onRefresh={vi.fn()}
                />
            </MemoryRouter>,
        )

        expect(
            screen.getByText(/verifizierte Nutzer bereit/i),
        ).toBeInTheDocument()
        expect(
            screen.getByRole('link', { name: /Zum Login/i }),
        ).toBeInTheDocument()
    })

    it('zeigt die Gesamtzusammenfassung und Hinweise ohne technische Internas an', () => {
        const onRefresh = vi.fn()

        render(
            <MemoryRouter>
                <AiWeatherAdvisorPanel
                    analysis={sampleAnalysis}
                    loading={false}
                    error={null}
                    isAuthenticated={true}
                    onRefresh={onRefresh}
                />
            </MemoryRouter>,
        )

        expect(
            screen.getByText(
                'Wechselhafte Bedingungen mit Seitenwind im mittleren Abschnitt',
            ),
        ).toBeInTheDocument()
        expect(
            screen.getByText(
                /Auf deiner Motorrad-Route treten ab Kilometer 10 kräftige Böenspitzen/i,
            ),
        ).toBeInTheDocument()
        expect(
            screen.getByText('Vor Fahrtantritt Visier-Belüftung öffnen.'),
        ).toBeInTheDocument()
        expect(
            screen.queryByText(/MCP/i),
        ).not.toBeInTheDocument()
    })
})
