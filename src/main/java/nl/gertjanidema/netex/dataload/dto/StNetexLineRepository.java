package nl.gertjanidema.netex.dataload.dto;

import org.springframework.data.repository.CrudRepository;

public interface StNetexLineRepository extends CrudRepository<StNetexLine, String> {

    public void deleteByFileSetId(String fileSetId);
}

