package nl.gertjanidema.netex.dataload.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "netex")
@Getter
@Setter
public class StNetexRoute {
    @Id
    private String id;
    private String name;
    private String lineRef;
    private String directionType;
    private String fileSetId;
}
