package nl.gertjanidema.netex.dataload;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import nl.gertjanidema.netex.core.util.RdToWgs84Transformation;
import nl.gertjanidema.netex.dataload.ndov.NdovService;

@Configuration
@EnableJpaRepositories(basePackages = "nl.gertjanidema.netex.dataload.dto")
@EntityScan("nl.gertjanidema.netex.dataload.dto")
public class MainConfiguration {
    @SuppressWarnings("static-method")
    @Bean
    GeometryFactory wsgGeometryFactory() {
        return new GeometryFactory(new PrecisionModel(), 4326);
    }
    
    @SuppressWarnings("static-method")
    @Bean
    GeometryFactory rdGeometryFactory() {
        return new GeometryFactory(new PrecisionModel(), 28992);
    }
    
    @SuppressWarnings("static-method")
    @Bean
    RdToWgs84Transformation transformation() {
        return new RdToWgs84Transformation();
    }

   @SuppressWarnings("static-method")
    @Bean
    NdovService ndovService() {
        return new NdovService();
    }
}
