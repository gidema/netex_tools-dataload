package nl.gertjanidema.netex.dataload.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.rutebanken.netex.model.PointInJourneyPattern;
import org.rutebanken.netex.model.ServiceJourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimingPointInJourneyPattern;

import nl.gertjanidema.netex.dataload.dto.StNetexPointOnJourney;

public class NetexPointOnJourneyProcessor {

    public static List<StNetexPointOnJourney> process(ServiceJourneyPattern journey) throws Exception {
        var journeyId = journey.getId();
        var routeId = journey.getRouteRef().getRef();
        var pointsInSequence = journey.getPointsInSequence();
        if (pointsInSequence == null) return null;
        var pointsOnJourney = journey.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
        var stPoints = new ArrayList<StNetexPointOnJourney>(pointsOnJourney.size());
        pointsOnJourney.forEach(poj -> {
            var stPoj = new StNetexPointOnJourney();
            stPoj.setJourneyId(journeyId);
            stPoj.setRouteId(routeId);
            stPoj.setPointOnJourneyId(poj.getId());
            // TODO Log when getOrder is null
            stPoj.setSequence(Objects.requireNonNullElse(poj.getOrder(), 0).intValue());
            if (poj instanceof StopPointInJourneyPattern) {
                StopPointInJourneyPattern stopPoint = (StopPointInJourneyPattern) poj;
                stPoj.setPointType("stop point");
                stPoj.setRoutePointRef(stopPoint.getScheduledStopPointRef().getValue().getRef());
            }
            else if (poj instanceof TimingPointInJourneyPattern) {
                TimingPointInJourneyPattern timingPoint = (TimingPointInJourneyPattern) poj;
                stPoj.setRoutePointRef(timingPoint.getTimingPointRef().getValue().getRef());
                stPoj.setPointType("timing point");
            }
            else if (poj instanceof PointInJourneyPattern) {
                PointInJourneyPattern point = (PointInJourneyPattern) poj;
                stPoj.setRoutePointRef(point.getPointRef().getValue().getRef());
                stPoj.setPointType("point");
            }
            else stPoj.setPointType("unknown");
            stPoints.add(stPoj);
        });
        return stPoints;
    }

}
