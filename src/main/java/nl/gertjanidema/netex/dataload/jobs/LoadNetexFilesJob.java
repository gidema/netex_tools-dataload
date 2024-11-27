package nl.gertjanidema.netex.dataload.jobs;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import nl.gertjanidema.netex.dataload.dto.NetexFileInfo;
import nl.gertjanidema.netex.dataload.dto.NetexTransportArea;
import nl.gertjanidema.netex.dataload.dto.NetexTransportAreaRepository;
import nl.gertjanidema.netex.dataload.ndov.NdovService;

@Configuration
@RequiredArgsConstructor
@EnableBatchProcessing
public class LoadNetexFilesJob {
    public List<String> fileGroupNames;

    private List<NetexFileInfo> netexFileInfo;

    @Inject
    private NdovService ndovService;

    /**
     * Defines the main batch job for importing.
     *
     * @param jobRepository the repository for storing job metadata.
     * @param truncateStep the step associated with this job.
     * @return a configured Job for exporting quays.
     */
    
    @SuppressWarnings("static-method")
    @Bean
    Job importJob(JobRepository jobRepository, Step readTransportAreasStep, Step readFileInfoStep,
            Step downloadStep)  {
        return new JobBuilder("LoadNetexFilesJob", jobRepository)
            .start(readTransportAreasStep)
            .next(readFileInfoStep)
            .next(downloadStep)
            .build();
    }

    @Bean
    Step readTransportAreasStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("readTransportAreaStep", jobRepository)
        .tasklet(readTransportAreas(), transactionManager)
        .allowStartIfComplete(true)
        .build();
    }

    /**
     * Defines the batch step to get the NeTex file information.
     *
     * @param jobRepository the repository for storing job metadata.
     * @param transactionManager the transaction manager to handle transactional behavior.
     * @return a configured Step for reading and writing Contact entities.
     */
    @Bean
    Step readFileInfoStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("readFileInfoStep", jobRepository)
        .tasklet(readFileInfo(), transactionManager)
        .allowStartIfComplete(true)
        .build();
    }

    @Bean
    Step downloadStep() {
        return new DownloadStep();
    }

    @Bean
    ReadTransportAreasTasklet readTransportAreas() {
        return new ReadTransportAreasTasklet();
    }
   
    @Bean
    ReadNetexFileinfoTasklet readFileInfo() {
        return new ReadNetexFileinfoTasklet();
    }
    
    class ReadTransportAreasTasklet implements Tasklet {
        @Inject
        private NetexTransportAreaRepository repository;

        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

            fileGroupNames = StreamSupport.stream(repository.findAll().spliterator(), false)
            .map(NetexTransportArea::getTransportArea)
            .collect(Collectors.toList());
            return RepeatStatus.FINISHED;
        }
    }

    class ReadNetexFileinfoTasklet implements Tasklet {
        
        @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            netexFileInfo = ndovService.getNetexFileInfo();
            return RepeatStatus.FINISHED;
        }
    }

    class DownloadStep extends AbstractStep {

        public DownloadStep() {
            this.setName("downloadStep");
            this.setAllowStartIfComplete(true);
        }

        @Override
        @Inject
        public void setJobRepository(JobRepository jobRepository) {
            super.setJobRepository(jobRepository);
        }
        
        //TODO this method contains NDOV specific code. Move it to NdovService
        @Override
        protected void doExecute(StepExecution stepExecution) throws Exception {
            Map<String, NetexFileInfo> latestFiles = new HashMap<>();
            netexFileInfo.forEach(fileInfo -> {
                var fileGroup = fileInfo.getFileGroup();
                if (fileGroupNames.contains(fileGroup)) {
                    var latestFileInfo = latestFiles.get(fileGroup);
                    if (latestFileInfo == null) {
                        latestFiles.put(fileGroup, fileInfo);
                    }
                    else if (fileInfo.getFileName().compareTo(latestFileInfo.getFileName()) > 0)  {
                        latestFiles.put(fileGroup, fileInfo);                        
                    }
                }
                
            });
            ndovService.downloadNetexFiles(latestFiles.values());
            stepExecution.setExitStatus(ExitStatus.COMPLETED);
            stepExecution.setEndTime(LocalDateTime.now());
        }
    }
}