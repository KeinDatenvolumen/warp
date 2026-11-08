// [KI-Log Nr. KI-159] (906871f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose internal route calculation API
// [KI-Log Nr. KI-175] (b18893e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose internal route calculation API
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.route.application;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RouteCalculationCreatedListener {

    private final RouteCalculationProcessor processor;

    public RouteCalculationCreatedListener(
            RouteCalculationProcessor processor
    ) {
        this.processor = processor;
    }

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            RouteCalculationCreatedEvent event
    ) {
        processor.process(event.calculationId());
    }
}