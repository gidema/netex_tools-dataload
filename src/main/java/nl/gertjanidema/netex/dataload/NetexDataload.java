package nl.gertjanidema.netex.dataload.cli;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.Transactional;

import jakarta.inject.Inject;
import nl.gertjanidema.netex.dataload.NetexFileProcessor;
import nl.gertjanidema.netex.dataload.dto.NetexFileInfo;
import nl.gertjanidema.netex.dataload.dto.StNetexDeliveryRepository;
import nl.gertjanidema.netex.dataload.ndov.NdovService;

@SpringBootApplication
@ComponentScan(basePackages = { "nl.gertjanidema.netex.dataload" },
excludeFilters = { @ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = "nl.gertjanidema.netex.dataload.jobs.*")})
public class NetexDataload implements CommandLineRunner {

    private static Logger LOG = LoggerFactory.getLogger(NetexDataload.class);

    @SuppressWarnings("resource")
    public static void main(String[] args) {
        LOG.info("STARTING THE APPLICATION");
        new SpringApplicationBuilder(NetexDataload.class)
            .web(WebApplicationType.NONE)
            .run(args);
        LOG.info("APPLICATION FINISHED");
    }
    
    @Inject
    NdovService ndovService;
    
    @Inject
    StNetexDeliveryRepository deliveryRepository;
    
    @Override
    public void run(String... args) {
        try {
            var newNetexFiles = ndovService.checkForNewNetexFiles();
            // Temporary for development
//            newNetexFiles = List.of(newNetexFiles.get(0), newNetexFiles.get(1));
            // Cache the requested netex files
            var files = ndovService.downloadNetexFiles(newNetexFiles);
            files.forEach(file -> {
                LOG.info("Processing file {}.", file.getFileName());
                processFile(file);
            });
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Transactional
    private void processFile(NetexFileInfo fileInfo) {
        var fileProcessor = fileProcessor();
        var newDelivery = fileProcessor.processHeader(fileInfo);
        var existingDelivery = deliveryRepository.findById(newDelivery.getFileSetId());
        if(existingDelivery.isEmpty() || 
            newDelivery.getFilename().compareTo(existingDelivery.get().getFilename()) > 0) {
            fileProcessor.processData();
            deliveryRepository.save(newDelivery);
        }
    }

    @SuppressWarnings("static-method")
    @Bean
    @Scope("prototype")
    NetexFileProcessor fileProcessor() {
        return new NetexFileProcessor();
    }
}