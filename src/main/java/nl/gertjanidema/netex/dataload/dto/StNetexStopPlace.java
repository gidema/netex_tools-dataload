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
public class StNetexStopPlace {
    @Id
    private String id;
    private String version;
    private String name;
    private String privateCode;
    private Double x;
    private Double y;
    private String fileSetId;
}
