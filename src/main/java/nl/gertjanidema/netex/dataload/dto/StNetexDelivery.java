package nl.gertjanidema.netex.dataload.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema="netex")
@Getter
@Setter
public class StNetexDelivery {
    @Id
    String fileSetId;
    String filename;
    LocalDateTime publicationTimestamp;
    OffsetDateTime downloadTimestamp;
    String participantRef;
    String description;
}
