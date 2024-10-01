package nl.gertjanidema.netex.dataload.jobs;

import org.rutebanken.netex.model.Line;

import nl.gertjanidema.netex.dataload.dto.StNetexLine;
import nl.gertjanidema.netex.dataload.processors.NetexLineProcessor;

//@Configuration
//@EnableBatchProcessing
public class NetexLineDataloadJob extends AbstractNetexDataloadJob<Line, StNetexLine>{

    public NetexLineDataloadJob() {
        super(new NetexLineProcessor(), Line.class,
                "netex.st_netex_line",
                "Line",
                "loadNetexLineJob");
    }
}