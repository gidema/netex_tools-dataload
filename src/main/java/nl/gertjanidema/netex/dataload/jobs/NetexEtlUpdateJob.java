package nl.gertjanidema.netex.dataload.jobs;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableBatchProcessing
public class NetexEtlUpdateJob {

private final static String update_netex_line_sql = """
    TRUNCATE TABLE netex.netex_line;
    INSERT INTO netex.netex_line(id, name, branding_ref, direction_type, transport_mode, public_code, private_code, colour, text_colour, mobility_impaired_access, responsibility_set, product_category, network)
    SELECT line."id",
        line."name",
        line.branding_ref,
        line.direction_type,
        line.transport_mode,
        line.public_code,
        line.private_code,
        line.colour,
        line.text_colour,
        line.mobility_impaired_access,
        ra.name AS responsibility_set,
        pc.name AS product_category,
        nw.network AS network
    FROM netex.st_netex_line line
    LEFT JOIN netex.st_netex_product_category pc ON pc.id = product_category_ref
    LEFT JOIN netex.st_netex_responsible_area ra ON ra.id = responsibility_set_ref
    LEFT JOIN netex.ref_netex_network nw ON nw.netex_network = ra.name;
""";

    private final static String update_netex_quay_sql = """
TRUNCATE TABLE netex.netex_quay;
INSERT INTO netex.netex_quay
SELECT "id",
    stop_area_id,
    "name",
    "short_name",
    ST_SETSRID(ST_POINT(x_coordinate, y_coordinate), 28992) AS rd_location,
    ST_TRANSFORM(ST_SETSRID(ST_POINT(x_coordinate, y_coordinate), 28992), 4326) AS wsg_location,
    user_stop_code,
    place,
    user_stop_owner_code,
    route_point_ref,
    for_boarding,
    for_alighting,
    tariff_zones
FROM netex.st_netex_scheduled_stop_point
""";
    
    private final static String update_netex_route_sql = """
TRUNCATE TABLE netex.netex_route;
INSERT INTO netex.netex_route
SELECT "id", "name", "line_ref", "direction_type"
FROM netex.st_netex_route
""";

    private final static String update_netex_route_quay_sql = """
TRUNCATE TABLE netex.netex_route_quay;
INSERT INTO netex.netex_route_quay
SELECT sub.public_code AS line_number,
  sub.route_id,
  sub.quay_code,
  sub.stop_side_code,
  sub.stopplace_code,
  sub.rank AS quay_index,
  CASE WHEN sub.rank=1 THEN 'start' WHEN sub.rank = sub.count THEN 'end' ELSE 'middle' END AS quay_location_type,
  sub.point_on_route_id
  FROM (
    SELECT
    line.public_code,
    por.route_id,
    por.point_on_route_id,
    psa.quay_code,
    chb_quay.stop_side_code,
    COALESCE(psa.stopplace_code, csp.stopplacecode) AS stopplace_code,
    ROW_NUMBER() OVER (
      PARTITION BY por.route_id
      ORDER BY por.sequence ASC
    ) AS "rank",
    COUNT(*) OVER (
      PARTITION BY por.route_id
    ) AS count

    FROM netex.st_netex_point_on_route por
    JOIN netex.netex_quay quay ON quay.route_point_ref = por.route_point_ref
    JOIN netex.netex_route route ON route.id = por.route_id
    JOIN netex.netex_line line ON route.line_ref = line.id
    LEFT JOIN chb.chb_psa psa ON psa.user_stop_owner_code = quay.user_stop_owner_code
        AND psa.user_stop_code = quay.user_stop_code
    LEFT JOIN chb.chb_quay ON chb_quay.quaycode = psa.quay_code
    LEFT JOIN chb.chb_stop_place csp ON csp.id = chb_quay.stop_place_id
    WHERE por.route_point_ref NOT IN (
      SELECT route_point_ref
      FROM netex.ref_netex_ignore_route_point
    )
    ORDER BY por.route_id, por.sequence) AS sub;
""";

    private final static String update_netex_route_data_sql = """
TRUNCATE TABLE netex.netex_route_data;
WITH stats AS (
  SELECT rq.line_number, 
    rt.id AS route_id,
    rt.line_ref,
    rt.direction_type,
    ARRAY_AGG(rq.quay_code ORDER BY rq.quay_index) quay_list,
    ARRAY_AGG(rq.stopplace_code ORDER BY rq.quay_index) stopplace_list,
    COUNT(rq.quay_code) AS quay_count
  FROM netex.netex_route rt
  LEFT JOIN netex.netex_route_quay rq ON rq.route_id = rt.id
  GROUP BY rq.line_number, rt.id, rt.line_ref)
INSERT INTO netex.netex_route_data (line_number, route_id, line_ref, direction_type, quay_list, stopplace_list, quay_count, start_quay_code, end_quay_code, start_stopplace_code, end_stopplace_code)
SELECT stats.*,
    start_quay.quay_code AS start_quay_code,
    end_quay.quay_code AS end_quay_code,
    start_quay.stopplace_code AS start_stopplace_code,
    end_quay.stopplace_code AS end_stopplace_code
FROM stats
JOIN netex.netex_route_quay start_quay ON start_quay.route_id = stats.route_id AND start_quay.quay_location_type = 'start'
JOIN netex.netex_route_quay end_quay ON end_quay.route_id = stats.route_id AND end_quay.quay_location_type = 'end';
""";

    private final static String update_netex_unique_route_sql = """
TRUNCATE TABLE netex.netex_unique_route;
INSERT INTO netex.netex_unique_route (line_number, direction_type, quay_list, stopplace_list, quay_count, start_quay_code, end_quay_code, start_stopplace_code, end_stopplace_code, line_ref, "count", route_refs)
SELECT line_number, direction_type, quay_list, stopplace_list, quay_count, start_quay_code, end_quay_code, start_stopplace_code, end_stopplace_code, line_ref, count(route_id), ARRAY_AGG(route_id) AS route_refs
FROM netex.netex_route_data
GROUP BY line_number, direction_type, quay_list, stopplace_list, quay_count, start_quay_code, end_quay_code, start_stopplace_code, end_stopplace_code, line_ref
""";

    private final static String update_netex_line_endpoint_sql = """
TRUNCATE TABLE netex.netex_line_endpoint;
INSERT INTO netex.netex_line_endpoint
SELECT DISTINCT *
FROM (
      SELECT line.id AS netex_line_id, rd.line_number, rd.start_stopplace_code AS stopplace_code
      FROM netex.netex_line line
        JOIN netex.netex_route route ON route.line_ref = line.id
        JOIN netex.netex_route_data rd ON rd.route_id = route.id
      WHERE line.transport_mode = 'bus'
    UNION 
      SELECT line.id AS netex_line_id, rd.line_number, rd.end_stopplace_code AS stopplace_code
      FROM netex.netex_line line
        JOIN netex.netex_route route ON route.line_ref = line.id
        JOIN netex.netex_route_data rd ON rd.route_id = route.id
      WHERE line.transport_mode = 'bus'
    ) AS SUB
    WHERE stopplace_code IS NOT NULL;
""";

    private final static String update_netex_links_sql = """
TRUNCATE TABLE netex.netex_link;
INSERT INTO netex.netex_link
SELECT DISTINCT rq1.quay_code AS quay_code1, rq1.stop_side_code AS stop_side_code1, rq1.stopplace_code AS stopplace_code1,
    rq2.quay_code AS quay_code2, rq2.stop_side_code AS stop_side_code2, rq2.stopplace_code AS stopplace_code2
FROM netex.netex_route_quay rq1
JOIN netex.netex_route_quay rq2 ON rq1.route_id = rq2.route_id AND rq2.quay_index = rq1.quay_index + 1
WHERE rq1.quay_code IS NOT NULL AND rq2.quay_code IS NOT NULL;        
""";
    
    private final EntityManagerFactory entityManagerFactory;

    @Inject
    private ApplicationContext applicationContext;

    @Bean
    EntityManager entityManager() {
        return entityManagerFactory.createEntityManager();
    }
    
    /**
     * Defines the main batch job for importing.
     *
     * @param jobRepository the repository for storing job metadata.
     * @param step1 the step associated with this job.
     * @return a configured Job for importing contacts.
     */
    @Bean
    Job netexEtlUpdate(JobRepository jobRepository)  {

        return new JobBuilder("netexEtlUpdateJob", jobRepository)
            .start(sqlUpdateStep("Update lines", update_netex_line_sql))
            .next(sqlUpdateStep("Update routes", update_netex_route_sql))
            .next(sqlUpdateStep("Update quays", update_netex_quay_sql))
            .next(sqlUpdateStep("Update routeQuays", update_netex_route_quay_sql))
            .next(sqlUpdateStep("Update routeData", update_netex_route_data_sql))
            .next(sqlUpdateStep("Update unique routes", update_netex_unique_route_sql))
            .next(sqlUpdateStep("Update line endpoints", update_netex_line_endpoint_sql))
            .next(sqlUpdateStep("Update netex links", update_netex_links_sql))
            .build();
    }

    /**
     * Defines an SQL update step to update a.
     *
     * @param stepName The name of the step
     * @param sql The sql code to execute.
     * @return a configured Step.
     */
    Step sqlUpdateStep(String stepName, String sql) {
        var jobRepository = applicationContext.getBean(JobRepository.class);
        var transactionManager = applicationContext.getBean(PlatformTransactionManager.class);
        var transactionTemplate = applicationContext.getBean(TransactionTemplate.class);
        Tasklet tasklet = sqlTasklet(transactionTemplate, sql);
        return new StepBuilder(stepName, jobRepository)
        .tasklet(tasklet, transactionManager)
        .allowStartIfComplete(true)
        .build();
    }

    @SuppressWarnings("static-method")
    @Bean 
    TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);       
    }
    
    private Tasklet sqlTasklet(TransactionTemplate transactionTemplate, String query) {
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