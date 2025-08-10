package nl.gertjanidema.netex.dataload.dto;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "netex")
@Getter
@Setter
public class StNetexScheduledStopPoint {
    @Id
    private String id;
    private String stopAreaId;
    private List<String> tariffZones;
    private String name;
    private String shortName;
    private String place;
    private String routePointRef;
    private Double xCoordinate;
    private Double yCoordinate;
    private String userStopCode;
    private String userStopOwnerCode;
    private Boolean forBoarding;
    private Boolean forAlighting;
    private String fileSetId;
}
