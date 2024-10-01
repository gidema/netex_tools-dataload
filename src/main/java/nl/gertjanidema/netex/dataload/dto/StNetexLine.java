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
public class StNetexLine {
    @Id
    private String id;
    private String responsibilitySetRef;
    private String name;
    private String brandingRef;
    private String directionType;
    private String productCategoryRef;
    private String transportMode;
    private String publicCode;
    private String privateCode;
    private String colour;
    private String textColour;
    private boolean mobilityImpairedAccess;
}
