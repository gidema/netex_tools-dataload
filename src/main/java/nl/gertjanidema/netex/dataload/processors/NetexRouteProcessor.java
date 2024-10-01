package nl.gertjanidema.netex.dataload.processors;

import org.rutebanken.netex.model.Route;
import org.springframework.batch.item.ItemProcessor;

import nl.gertjanidema.netex.dataload.dto.StNetexRoute;

public class NetexRouteProcessor implements ItemProcessor<Route, StNetexRoute> {
 
    @Override
    public StNetexRoute process(Route route) throws Exception {
        var netexRoute = new StNetexRoute();
        netexRoute.setId(route.getId());
        netexRoute.setName(route.getName() != null ? route.getName().getValue() : null);
        netexRoute.setLineRef(route.getLineRef().getValue().getRef());
        netexRoute.setDirectionType(route.getDirectionType().toString());
        return netexRoute;       
    }
}