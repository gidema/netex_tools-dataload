package nl.gertjanidema.netex.dataload.dto;

import java.util.Calendar;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema="netex")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NetexFileInfo {
    private String agencyId;
    private String fileGroup;
    @Id
    @EqualsAndHashCode.Include
    private String fileName;
    private Calendar lastModified;
    private Long size;
}
