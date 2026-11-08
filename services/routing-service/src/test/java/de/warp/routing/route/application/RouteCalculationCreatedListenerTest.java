// [KI-Log Nr. KI-159] (906871f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose internal route calculation API
// [KI-Log Nr. KI-175] (b18893e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose internal route calculation API
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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