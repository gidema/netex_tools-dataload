package nl.gertjanidema.netex.dataload.processors;

import org.rutebanken.netex.model.StopPlace;

import nl.gertjanidema.netex.dataload.dto.StNetexStopPlace;

public class NetexStopPlaceProcessor {
    public static StNetexStopPlace process(StopPlace stopPlace) throws Exception {
        var netexStopPlace = new StNetexStopPlace();
        netexStopPlace.setId(stopPlace.getId());
        netexStopPlace.setVersion(stopPlace.getVersion());
        netexStopPlace.setName(stopPlace.getName().getValue());
        netexStopPlace.setPrivateCode(stopPlace.getPrivateCode().getValue());
        if (stopPlace.getCentroid() != null) {
            var position = stopPlace.getCentroid().getLocation().getPos();
            netexStopPlace.setX(position.getValue().get(0));
            netexStopPlace.setY(position.getValue().get(0));
        }
        return netexStopPlace;
    }
}
