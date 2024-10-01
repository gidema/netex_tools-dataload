package nl.gertjanidema.netex.dataload.processors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rutebanken.netex.model.PointProjection;
import org.rutebanken.netex.model.ScheduledStopPoint;
import org.springframework.batch.item.ItemProcessor;

import nl.gertjanidema.netex.dataload.dto.StNetexScheduledStopPoint;

public class NetexScheduledStopPointProcessor implements ItemProcessor<ScheduledStopPoint, StNetexScheduledStopPoint> {
 
    @Override
    public StNetexScheduledStopPoint process(ScheduledStopPoint stop) throws Exception {
        var stopPoint = new StNetexScheduledStopPoint();
        stopPoint.setId(stop.getId());
        var stopName = getStopName(stop);
        stopPoint.setName(stopName.name());
        stopPoint.setPlace(stopName.place());
        if (stop.getShortName() != null) {
            stopPoint.setShortName(stop.getShortName().getValue());
        }
        if (stop.getPrivateCode() != null && "UserStopCode".equals(stop.getPrivateCode().getType())) {
            stopPoint.setUserStopCode(stop.getPrivateCode().getValue());
            stopPoint.setUserStopOwnerCode(getOwnerCodeFromId(stop.getId()));
        }
        List<Double> position = getPosition(stop);
        stopPoint.setXCoordinate(position.get(0));
        stopPoint.setYCoordinate(position.get(1));
        stopPoint.setRoutePointRef(getRoutePointRef(stop));
        stopPoint.setTariffZones(getTariffZones(stop));
        stopPoint.setForBoarding(stop.isForBoarding() == null ? true : stop.isForBoarding()); 
        stopPoint.setForAlighting(stop.isForAlighting() == null ? true : stop.isForAlighting());
        return stopPoint;
    }
    
    private static StopName getStopName(ScheduledStopPoint stop) {
        if (stop.getName() == null) return null;
        var parts = stop.getName().getValue().split(", ", 2);
        if (parts.length ==2) {
            return new StopName(parts[1], parts[0]);
        }
        return new StopName(parts[0], null);                
    }
    
    private static List<Double> getPosition(ScheduledStopPoint stop) {
        var rd = stop.getLocation();
        if (rd == null) return null;
        return rd.getPos().getValue();
    }
    
    private static String getRoutePointRef(ScheduledStopPoint stop) {
        if (stop.getProjections() != null) {
            for (var jaxbElement : stop.getProjections().getProjectionRefOrProjection()) {
                if (jaxbElement.getDeclaredType().equals(PointProjection.class)) {
                    var pointProjection = (PointProjection)jaxbElement.getValue();
                    var ptpRef = pointProjection.getProjectToPointRef();
                    return (ptpRef == null ? null : ptpRef.getRef());
                }
            }
        }
        return null;
    }
    
    private static List<String> getTariffZones(ScheduledStopPoint stop) {
        if(stop.getTariffZones() == null) return Collections.emptyList(); 
        var zoneRefs = stop.getTariffZones().getTariffZoneRef_();
        List<String> zones = new ArrayList<>(zoneRefs.size());
        zoneRefs.forEach(z -> {
            var zoneRef = z.getValue();
            String[] parts = zoneRef.getRef().split(":");
            if (parts.length == 3 && "DOVA".equals(parts[0])) {
                zones.add(parts[2]);
            }
        });
        return zones;
    }
    
    private static String getOwnerCodeFromId(String id) {
        return id.split(":")[0];
    }
    
    static record StopName(String name, String place) {}
}