package de.warp.routing.route.application;

public class RouteCalculationNotFoundException
        extends RuntimeException {

    public RouteCalculationNotFoundException(
            String calculationId
    ) {
        super(
                "Die Routenberechnung wurde nicht gefunden: "
                        + calculationId
        );
    }
}