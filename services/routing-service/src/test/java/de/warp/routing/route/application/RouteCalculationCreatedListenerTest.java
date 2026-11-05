// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.route.application;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteCalculationCreatedListenerTest {

    @Mock
    private RouteCalculationProcessor processor;

    private RouteCalculationCreatedListener listener;

    @BeforeEach
    void setUp() {
        listener = new RouteCalculationCreatedListener(
                processor
        );
    }

    @Test
    void processesCreatedRouteCalculation() {
        listener.handle(
                new RouteCalculationCreatedEvent(
                        "calc_123"
                )
        );

        verify(processor).process("calc_123");
    }
}