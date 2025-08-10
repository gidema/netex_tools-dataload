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
public class StNetexQuay {
    @Id
    private String id;
    private String stopPlaceId;
    private String version;
    private String name;
    private Double x;
    private Double y;
    private String quayType;
    private String fileSetId;
}
