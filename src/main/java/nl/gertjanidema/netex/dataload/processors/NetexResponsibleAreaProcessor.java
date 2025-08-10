package nl.gertjanidema.netex.dataload.processors;

import org.rutebanken.netex.model.ResponsibilitySet;

import nl.gertjanidema.netex.dataload.dto.StNetexResponsibleArea;

public class NetexResponsibleAreaProcessor {
 
    public static StNetexResponsibleArea process(ResponsibilitySet responsibilitySet) throws Exception {
        var netexArea = new StNetexResponsibleArea();
        netexArea.setId(responsibilitySet.getId());
        netexArea.setName(responsibilitySet.getName() != null ? responsibilitySet.getName().getValue() : null);
        return netexArea;
    }
}