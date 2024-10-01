package nl.gertjanidema.netex.dataload.jobs;

import java.io.IOException;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import nl.gertjanidema.netex.core.batch.GzipAwareMultiResourceItemReader;
import nl.gertjanidema.netex.dataload.ndov.NdovService;

/**
 * @param <T> The source class as it is defined in the JAXB package
 * @param <S> The staging class reflecting the DTO object that is saved to the staging area
 */
@Configuration
@RequiredArgsConstructor
@EnableBatchProcessing
public abstract class AbstractNetexDataloadJob<T, S> {

    @Inject
    NdovService ndovService;

    @Inject
    EntityManagerFactory entityManagerFactory;
    
    private final ItemProcessor<T, S> processor;
    private final Class<T> clazz;
    private final String table;
    private final String fragmentRootElement;
    private final String jobName;

    
    @Bean
    @StepScope
    ItemReader<T> multiResourceReader() {
        var path = ndovService.getNetexTempPath();
        var patternResolver = new PathMatchingResourcePatternResolver();   
        try {
            Resource[] resources = patternResolver.getResources("file:" + path + "/*.xml.gz");
            var reader = new GzipAwareMultiResourceItemReader<T>();
            reader.setResources(resources);
            reader.setDelegate(itemReader());
            return reader;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Creates and returns a {@link JpaItemWriter} bean for persisting entities.
     *
     * @return a configured JpaItemWriter for writing entities.
     */
    @Bean
    JpaItemWriter<S> writer() {
        var writer = new JpaItemWriter<S>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    @Bean
    @StepScope
    StaxEventItemReader<T> itemReader() {
        return new StaxEventItemReaderBuilder<T>()
            .name("itemReader")
            .addFragmentRootElements(fragmentRootElement)
            .unmarshaller(responsibilitySetMarshaller())
            .build();
    }

    @Bean
    Jaxb2Marshaller responsibilitySetMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setMappedClass(clazz);
        marshaller.setClassesToBeBound(clazz);
        return marshaller;
    }

    /**
     * Defines the main batch job for exporting.
     *
     * @param jobRepository the repository for storing job metadata.
     * @param truncateStep the step associated with this job.
     * @return a configured Job for exporting quays.
     */
    @Bean
    Job importJob(JobRepository jobRepository, 
            @Qualifier("truncateStep") Step truncateStep,
            @Qualifier("importStep") Step importStep)  {
        return new JobBuilder(jobName, jobRepository)
            .start(truncateStep)
            .next(importStep)
            .build();
    }

    /**
     * Step to remove existing quays from the database.
     *
     * @param jobRepository the repository for storing job metadata.
     * @param transactionManager the transaction manager to handle transactional behavior.
     * @return a configured Step for reading and writing Contact entities.
     */
    @SuppressWarnings("static-method")
    @Bean
    Step truncateStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            Tasklet routeTruncater) {
        return new StepBuilder("truncateStep", jobRepository)
            .tasklet(routeTruncater, transactionManager)
            .build();
    }
    /**
     * Defines the main batch step which includes reading, processing (if any), and writing.
     *
     * @param jobRepository the repository for storing job metadata.
     * @param transactionManager the transaction manager to handle transactional behavior.
     * @return a configured Step for reading and writing Contact entities.
     */
    @Bean
    Step importStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("importStep", jobRepository)
            .<T, S>chunk(1000, transactionManager)
            .reader(multiResourceReader())
            .processor(processor)
            .writer(writer())
            .build();
    }
    
    @Bean
    Tasklet routeTruncater(TransactionTemplate transactionTemplate) {
        return sqlTasklet(transactionTemplate, String.format("TRUNCATE %s;", table));
    }

    Tasklet sqlTasklet(TransactionTemplate transactionTemplate, String query) {
        return new Tasklet() {

            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                try (
                        var entityManager = entityManagerFactory.createEntityManager();
                )
                {
                    transactionTemplate.execute(transactionStatus -> {
                        entityManager.joinTransaction();
                        entityManager
                          .createNativeQuery(query)
                          .executeUpdate();
                        transactionStatus.flush();
                        return null;
                    });
                }
                finally {
                    //
                }
                return null;
            }
            
        };
    }
}