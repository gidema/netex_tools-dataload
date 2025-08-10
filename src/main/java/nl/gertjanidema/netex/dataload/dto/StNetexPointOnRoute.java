package nl.gertjanidema.netex.dataload.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema="netex")
@Getter
@Setter
public class StNetexPointOnRoute {
    @Id
    private String pointOnRouteId;
    private String routeId;
    private Integer sequence;
    private String routePointRef;
    private String fileSetId;
}
