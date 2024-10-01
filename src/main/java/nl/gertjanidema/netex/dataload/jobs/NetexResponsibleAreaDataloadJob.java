package nl.gertjanidema.netex.dataload.jobs;

import org.rutebanken.netex.model.ResponsibilitySet;

import nl.gertjanidema.netex.dataload.dto.StNetexResponsibleArea;
import nl.gertjanidema.netex.dataload.processors.NetexResponsibleAreaProcessor;

//@Configuration
//@EnableBatchProcessing
public class NetexResponsibleAreaDataloadJob extends AbstractNetexDataloadJob<ResponsibilitySet, StNetexResponsibleArea>{

    public NetexResponsibleAreaDataloadJob() {
        super(new NetexResponsibleAreaProcessor(), ResponsibilitySet.class,
                "netex.st_netex_responsible_area",
                "ResponsibilitySet",
                "loadNetexResponsibleAreaJob");
    }
}