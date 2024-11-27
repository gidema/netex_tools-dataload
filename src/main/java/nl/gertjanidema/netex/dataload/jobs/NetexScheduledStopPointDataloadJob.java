package nl.gertjanidema.netex.dataload.jobs;

import org.rutebanken.netex.model.ScheduledStopPoint;

import nl.gertjanidema.netex.dataload.dto.StNetexScheduledStopPoint;
import nl.gertjanidema.netex.dataload.processors.NetexScheduledStopPointProcessor;

public class NetexScheduledStopPointDataloadJob extends AbstractNetexDataloadJob<ScheduledStopPoint, StNetexScheduledStopPoint>{

    public NetexScheduledStopPointDataloadJob() {
        super(new NetexScheduledStopPointProcessor(),
            ScheduledStopPoint.class,
            "netex.st_netex_point_on_route",
            "ScheduledStopPoint",
            "loadNetexScheduledStopPointJob");
    }
}