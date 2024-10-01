package nl.gertjanidema.netex.dataload.processors;

import java.util.ArrayList;
import java.util.List;

import org.rutebanken.netex.model.Route;
import org.springframework.batch.item.ItemProcessor;

import nl.gertjanidema.netex.dataload.dto.StNetexPointOnRoute;

public class NetexPointOnRouteProcessor implements ItemProcessor<Route, List<StNetexPointOnRoute>> {

    @Override
    public List<StNetexPointOnRoute> process(Route route) throws Exception {
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
            stPor.setSequence(por.getOrder().intValue());
            stPoints.add(stPor);
        });
        return stPoints;
    }

}
