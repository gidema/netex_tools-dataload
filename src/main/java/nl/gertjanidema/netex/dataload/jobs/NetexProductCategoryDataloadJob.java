package nl.gertjanidema.netex.dataload.jobs;

import org.rutebanken.netex.model.TypeOfProductCategory;

import nl.gertjanidema.netex.dataload.dto.StNetexProductCategory;
import nl.gertjanidema.netex.dataload.processors.NetexProductCategoryProcessor;

//@Configuration
//@EnableBatchProcessing
public class NetexProductCategoryDataloadJob extends AbstractNetexDataloadJob<TypeOfProductCategory, StNetexProductCategory>{

    public NetexProductCategoryDataloadJob() {
        super(new NetexProductCategoryProcessor(), TypeOfProductCategory.class,
                "netex.st_netex_product_category",
                "TypeOfProductCategory",
                "loadNetexProductCategoryJob");
    }
}