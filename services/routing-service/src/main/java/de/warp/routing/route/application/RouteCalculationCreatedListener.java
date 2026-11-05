// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
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