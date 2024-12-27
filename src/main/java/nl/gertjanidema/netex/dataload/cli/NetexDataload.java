package nl.gertjanidema.netex.dataload.cli;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(basePackages = { "nl.gertjanidema.netex.dataload" },
excludeFilters = { @ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = "nl.gertjanidema.netex.dataload.jobs.*")})
public class NetexDataload implements ApplicationContextAware, CommandLineRunner {

    private static Logger LOG = LoggerFactory
      .getLogger(NetexDataload.class);

    @SuppressWarnings("resource")
    public static void main(String[] args) {
        LOG.info("STARTING THE APPLICATION");
        new SpringApplicationBuilder(NetexDataload.class)
            .web(WebApplicationType.NONE)
            .run(args);
        LOG.info("APPLICATION FINISHED");
    }

    private ApplicationContext applicationContext;

    @Override
    public void run(String... args) {
        boolean dataload = args.length == 0 || !args[0].equals("dl=no");
        LOG.info("EXECUTING : command line runner");
        var jobRegistry = applicationContext.getBean(JobRegistry.class);
        var jobLauncher = applicationContext.getBean(JobLauncher.class);
        Job job;
        try {
            var parameters = new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .toJobParameters();
            if (dataload) {
                job = jobRegistry.getJob("LoadNetexFilesJob");
                jobLauncher.run(job, parameters);
                job = jobRegistry.getJob("loadNetexScheduledStopPointJob");
                jobLauncher.run(job, parameters);
                job = jobRegistry.getJob("loadNetexLineJob");
                jobLauncher.run(job, parameters);
                job = jobRegistry.getJob("loadNetexRouteJob");
                jobLauncher.run(job, parameters);
                job = jobRegistry.getJob("loadNetexPointOnRouteJob");
                jobLauncher.run(job, parameters);
                job = jobRegistry.getJob("loadNetexResponsibleAreaJob");
                jobLauncher.run(job, parameters);
                job = jobRegistry.getJob("loadNetexProductCategoryJob");
                jobLauncher.run(job, parameters);
            }
            job = jobRegistry.getJob("netexEtlUpdateJob");
            jobLauncher.run(job, parameters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}