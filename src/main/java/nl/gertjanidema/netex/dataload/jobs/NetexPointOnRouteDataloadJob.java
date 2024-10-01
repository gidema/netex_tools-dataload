package nl.gertjanidema.netex.dataload.jobs;

import org.rutebanken.netex.model.Route;

import nl.gertjanidema.netex.dataload.dto.StNetexPointOnRoute;
import nl.gertjanidema.netex.dataload.processors.NetexPointOnRouteProcessor;
//@Configuration
//@EnableBatchProcessing
public class NetexPointOnRouteDataloadJob extends AbstractNetexListDataloadJob<Route, StNetexPointOnRoute>{

    public NetexPointOnRouteDataloadJob() {
        super(new NetexPointOnRouteProcessor(), Route.class,
                "netex.st_netex_point_on_route",
                "Route",
                "loadNetexPointOnRouteJob");
    }
}