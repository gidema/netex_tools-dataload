package nl.gertjanidema.netex.dataload.processors;

import org.rutebanken.netex.model.PublicationDeliveryStructure;

import nl.gertjanidema.netex.dataload.dto.NetexFileInfo;
import nl.gertjanidema.netex.dataload.dto.StNetexDelivery;

public class NetexDeliveryProcesser {
    public static StNetexDelivery process(PublicationDeliveryStructure delivery, NetexFileInfo fileInfo) {
        var stDelivery = new StNetexDelivery();
        stDelivery.setFileSetId(fileInfo.getFileSetId());
        stDelivery.setFilename(fileInfo.getFileName());
        stDelivery.setPublicationTimestamp(delivery.getPublicationTimestamp());
        stDelivery.setParticipantRef(delivery.getParticipantRef());
        if (delivery.getDescription() != null) {
            stDelivery.setDescription(delivery.getDescription().getValue());
        }
       return stDelivery;
    }
}
