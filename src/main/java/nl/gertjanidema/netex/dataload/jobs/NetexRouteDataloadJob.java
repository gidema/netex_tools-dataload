package nl.gertjanidema.netex.dataload.jobs;

import org.rutebanken.netex.model.Route;

import nl.gertjanidema.netex.dataload.dto.StNetexRoute;
import nl.gertjanidema.netex.dataload.processors.NetexRouteProcessor;


//@Configuration
//@EnableBatchProcessing
public class NetexRouteDataloadJob extends AbstractNetexDataloadJob<Route, StNetexRoute>{

    public NetexRouteDataloadJob() {
        super(new NetexRouteProcessor(), Route.class,
                "netex.st_netex_route",
                "Route",
                "loadNetexRouteJob");
    }
}