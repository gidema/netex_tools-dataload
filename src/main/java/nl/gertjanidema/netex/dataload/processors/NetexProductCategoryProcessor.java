package nl.gertjanidema.netex.dataload.processors;

import org.rutebanken.netex.model.TypeOfProductCategory;

import nl.gertjanidema.netex.dataload.dto.StNetexProductCategory;

public class NetexProductCategoryProcessor {
 
    public static StNetexProductCategory process(TypeOfProductCategory category) throws Exception {
        var netexProductCategory = new StNetexProductCategory();
        netexProductCategory.setId(category.getId());
        netexProductCategory.setName(category.getName() != null ? category.getName().getValue() : null);
        var description = category.getDescription();
        if (description != null) {
            netexProductCategory.setDescription(description.getValue());
        }
        return netexProductCategory;
    }
}