package nl.gertjanidema.netex.dataload.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.rutebanken.netex.model.Route;

import nl.gertjanidema.netex.dataload.dto.StNetexPointOnRoute;

public class NetexPointOnRouteProcessor {

    public static List<StNetexPointOnRoute> process(Route route) throws Exception {
        var routeId = route.getId();
        var pointsInSequence = route.getPointsInSequence();
        if (pointsInSequence == null) return null;
        var pointsOnRoute = route.getPointsInSequence().getPointOnRoute();
        var stPoints = new ArrayList<StNetexPointOnRoute>(pointsOnRoute.size());
        pointsOnRoute.forEach(por -> {
            var stPor = new StNetexPointOnRoute();
            stPor.setRouteId(routeId);
            stPor.setPointOnRouteId(por.getId());
            stPor.setRoutePointRef(por.getPointRef().getValue().getRef());
            // TODO Log when getOrder is null
            stPor.setSequence(Objects.requireNonNullElse(por.getOrder(), 0).intValue());
            stPoints.add(stPor);
        });
        return stPoints;
    }

}
