package nl.gertjanidema.netex.dataload.jobs;

import org.rutebanken.netex.model.ServiceJourneyPattern;

import nl.gertjanidema.netex.dataload.dto.StNetexPointOnJourney;
import nl.gertjanidema.netex.dataload.processors.NetexPointOnJourneyProcessor;
//@Configuration
//@EnableBatchProcessing
public class NetexPointOnJourneyDataloadJob extends AbstractNetexListDataloadJob<ServiceJourneyPattern, StNetexPointOnJourney>{

    public NetexPointOnJourneyDataloadJob() {
        super(new NetexPointOnJourneyProcessor(), ServiceJourneyPattern.class,
                "netex.st_netex_point_on_journey",
                "ServiceJourneyPattern",
                "loadNetexPointOnJourneyJob");
    }
}