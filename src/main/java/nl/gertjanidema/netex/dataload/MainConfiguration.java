package nl.gertjanidema.netex.dataload;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import nl.gertjanidema.netex.dataload.ndov.NdovService;

@Configuration
@EnableJpaRepositories(basePackages = "nl.gertjanidema.netex.dataload.dto")
@EntityScan("nl.gertjanidema.netex.dataload.dto")
public class MainConfiguration {
   @SuppressWarnings("static-method")
    @Bean
    NdovService ndovService() {
        return new NdovService();
    }
}
