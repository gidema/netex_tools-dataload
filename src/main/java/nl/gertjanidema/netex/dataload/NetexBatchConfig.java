package nl.gertjanidema.netex.dataload;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableBatchProcessing(modular = true)
public class NetexBatchConfig {
    
//    @SuppressWarnings("static-method")
//    @Bean
//    ApplicationContextFactory importNetexFileInfoConfig() {
//        return new GenericApplicationContextFactory(LoadNetexFilesJob.class);
//    }
//    
//    @SuppressWarnings("static-method")
//    @Bean
//    ApplicationContextFactory importNetexScheduledStopPointsConfig() {
//        return new GenericApplicationContextFactory(NetexScheduledStopPointDataloadJob.class);
//    }
//    
//    @SuppressWarnings("static-method")
//    @Bean
//    ApplicationContextFactory importNetexPointOnRouteConfig() {
//        return new GenericApplicationContextFactory(NetexPointOnRouteDataloadJob.class);
//    }
//    
//    @SuppressWarnings("static-method")
//    @Bean
//    ApplicationContextFactory importNetexPointOnJourneyConfig() {
//        return new GenericApplicationContextFactory(NetexPointOnJourneyDataloadJob.class);
//    }
//    
//    @SuppressWarnings("static-method")
//    @Bean
//    ApplicationContextFactory importNetexProductCategoryConfig() {
//        return new GenericApplicationContextFactory(NetexProductCategoryDataloadJob.class);
//    }
//    
//    @SuppressWarnings("static-method")
//    @Bean
//    ApplicationContextFactory loadNetexlinesConfig() {
//        return new GenericApplicationContextFactory(NetexLineDataloadJob.class);
//    }
//    
//    @SuppressWarnings("static-method")
//    @Bean
//    ApplicationContextFactory importNetexRoutesConfig() {
//        return new GenericApplicationContextFactory(NetexRouteDataloadJob.class);
//    }
//    
//    @SuppressWarnings("static-method")
//    @Bean
//    ApplicationContextFactory importNetexResponsibleAreaConfig() {
//        return new GenericApplicationContextFactory(NetexResponsibleAreaDataloadJob.class);
//    }
//    
//    @SuppressWarnings("static-method")
//    @Bean
//    ApplicationContextFactory updateNetexEtlConfig() {
//        return new GenericApplicationContextFactory(NetexEtlUpdateJob.class);
//    } 
}
