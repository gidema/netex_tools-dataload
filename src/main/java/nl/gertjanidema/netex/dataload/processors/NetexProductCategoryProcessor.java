package nl.gertjanidema.netex.dataload.processors;

import org.rutebanken.netex.model.TypeOfProductCategory;
import org.springframework.batch.item.ItemProcessor;

import nl.gertjanidema.netex.dataload.dto.StNetexProductCategory;

public class NetexProductCategoryProcessor implements ItemProcessor<TypeOfProductCategory, StNetexProductCategory> {
 
    @Override
    public StNetexProductCategory process(TypeOfProductCategory route) throws Exception {
        var netexProductCategory = new StNetexProductCategory();
        netexProductCategory.setId(route.getId());
        netexProductCategory.setName(route.getName() != null ? route.getName().getValue() : null);
        var description = route.getDescription();
        if (description != null) {
            netexProductCategory.setDescription(description.getValue());
        }
        return netexProductCategory;
    }
}