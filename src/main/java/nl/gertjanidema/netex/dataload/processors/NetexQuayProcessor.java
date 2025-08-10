package nl.gertjanidema.netex.dataload.processors;

import org.rutebanken.netex.model.Quay;

import nl.gertjanidema.netex.dataload.dto.StNetexQuay;
import nl.gertjanidema.netex.dataload.dto.StNetexStopPlace;

public class NetexQuayProcessor {
    public static StNetexQuay process(Quay quay, StNetexStopPlace stopPlace) throws Exception {
        var netexQuay = new StNetexQuay();
        netexQuay.setId(quay.getId());
        netexQuay.setStopPlaceId(stopPlace.getId());
        netexQuay.setVersion(quay.getVersion());
        netexQuay.setName(quay.getName().getValue());
        if (quay.getCentroid() != null) {
            var pos = quay.getCentroid().getLocation().getPos();
            netexQuay.setX(pos.getValue().get(0));
            netexQuay.setY(pos.getValue().get(1));
        }
        return netexQuay;
    }
}
