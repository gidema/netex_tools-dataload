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
    tariff_zones,
    id As point_id
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
WITH route_quay AS (
    SELECT
      line.public_code,
      por.route_id,
      por.point_on_route_id,
      psa.quay_code,
      chb_quay.stop_side_code,
      por.sequence AS sequence,
      COALESCE(psa.stop_place_code, csp.stop_place_code) AS stop_place_code
    FROM netex.st_netex_point_on_route por
    JOIN netex.netex_quay quay ON quay.route_point_ref = por.route_point_ref
    JOIN netex.netex_route route ON route.id = por.route_id
    JOIN netex.netex_line line ON route.line_ref = line.id
    LEFT JOIN chb.chb_psa psa ON psa.user_stop_owner_code = quay.user_stop_owner_code
        AND psa.user_stop_code = quay.user_stop_code
    LEFT JOIN chb.chb_quay ON chb_quay.quay_code = psa.quay_code
    LEFT JOIN chb.chb_stop_place csp ON csp.id = chb_quay.stop_place_id
    ORDER BY por.route_id, por.sequence
),
ignorable_point_on_route AS (
    SELECT rq1.point_on_route_id
    FROM route_quay rq1
    JOIN route_quay rq2 ON rq1.route_id = rq2.route_id
      AND rq1.stop_place_code = rq2.stop_place_code
      AND ABS(rq2.sequence - rq1.sequence) = 1
    WHERE rq1.quay_code IN('NL:Q:31002666','NL:Q:31006447','NL:Q:31007120','NL:Q:31000674','NL:Q:31003945','NL:Q:31002241',
      'NL:Q:31006485','NL:Q:31006277','NL:Q:42059710','NL:Q:31000408','NL:Q:31000329','310006667')
    UNION
    SELECT por.point_on_route_id
    FROM netex.st_netex_point_on_route por
    JOIN netex.ref_netex_ignore_route_point rp ON rp.route_point_ref = por.route_point_ref
    ORDER BY point_on_route_id ASC LIMIT 100
),
route_quay2 AS (
    SELECT
        public_code,
        route_id,
        point_on_route_id,
        quay_code,
        stop_side_code,
        stop_place_code,
        ROW_NUMBER() OVER (
          PARTITION BY route_id
          ORDER BY sequence ASC
        ) AS "rank",
        COUNT(*) OVER (
          PARTITION BY route_id
        ) AS count
    FROM route_quay
    WHERE point_on_route_id NOT IN (
        SELECT point_on_route_id FROM ignorable_point_on_route
    )
)
INSERT INTO netex.netex_route_quay
SELECT rq.public_code AS line_number,
  rq.route_id,
  rq.quay_code,
  rq.stop_side_code,
  rq.stop_place_code,
  rq.rank AS quay_index,
  CASE WHEN rq.rank=1 THEN 'start' WHEN rq.rank = rq.count THEN 'end' ELSE 'middle' END AS quay_location_type,
  rq.point_on_route_id
  FROM route_quay2 rq;
""";

    private final static String update_netex_journey_quay_sql = """
TRUNCATE TABLE netex.netex_journey_quay;
WITH route_quay AS (
    SELECT
      line.public_code,
      poj.route_id,
      poj.point_on_journey_id AS point_on_route_id,
      psa.quay_code,
      chb_quay.stop_side_code,
      poj.sequence AS sequence,
      COALESCE(psa.stop_place_code, csp.stop_place_code) AS stop_place_code
    FROM netex.st_netex_point_on_journey poj
    JOIN netex.netex_quay quay ON quay.point_id = poj.route_point_ref
    JOIN netex.netex_route route ON route.id = poj.route_id
    JOIN netex.netex_line line ON route.line_ref = line.id
    LEFT JOIN chb.chb_psa psa ON psa.user_stop_owner_code = quay.user_stop_owner_code
        AND psa.user_stop_code = quay.user_stop_code
    LEFT JOIN chb.chb_quay ON chb_quay.quay_code = psa.quay_code
    LEFT JOIN chb.chb_stop_place csp ON csp.id = chb_quay.stop_place_id
    WHERE poj.point_type = 'stop point'
),
ignorable_point_on_route AS (
    SELECT rq1.point_on_route_id
    FROM route_quay rq1
    JOIN route_quay rq2 ON rq1.route_id = rq2.route_id
      AND rq1.stop_place_code = rq2.stop_place_code
      AND ABS(rq2.sequence - rq1.sequence) = 1
    WHERE rq1.quay_code IN('NL:Q:31002666','NL:Q:31006447','NL:Q:31007120','NL:Q:31000674','NL:Q:31003945','NL:Q:31002241',
      'NL:Q:31006485','NL:Q:31006277','NL:Q:42059710','NL:Q:31000408','NL:Q:31000329','310006667')
    UNION
    SELECT poj.point_on_journey_id
    FROM netex.st_netex_point_on_journey poj
    JOIN netex.ref_netex_ignore_route_point rp ON rp.scheduled_stop_point_ref = poj.route_point_ref
),
route_quay2 AS (
    SELECT
        public_code,
        route_id,
        point_on_route_id,
        quay_code,
        stop_side_code,
        stop_place_code,
        ROW_NUMBER() OVER (
          PARTITION BY route_id
          ORDER BY sequence ASC
        ) AS "rank",
        COUNT(*) OVER (
          PARTITION BY route_id
        ) AS count
    FROM route_quay
    WHERE point_on_route_id NOT IN (
        SELECT point_on_route_id FROM ignorable_point_on_route
    )
)
INSERT INTO netex.netex_route_quay
SELECT rq.public_code AS line_number,
  rq.route_id,
  rq.quay_code,
  rq.stop_side_code,
  rq.stop_place_code,
  rq.rank AS quay_index,
  CASE WHEN rq.rank=1 THEN 'start' WHEN rq.rank = rq.count THEN 'end' ELSE 'middle' END AS quay_location_type,
  rq.point_on_route_id
  FROM route_quay2 rq;
""";

    private final static String update_netex_route_data_sql = """
TRUNCATE TABLE netex.netex_route_data;
WITH stats AS (
  SELECT rq.line_number, 
    rt.id AS route_id,
    rt.line_ref,
    rt.direction_type,
    ARRAY_AGG(rq.quay_code ORDER BY rq.quay_index) quay_list,
    ARRAY_AGG(rq.stop_place_code ORDER BY rq.quay_index) stop_place_list,
    COUNT(rq.quay_code) AS quay_count
  FROM netex.netex_route rt
  LEFT JOIN netex.netex_route_quay rq ON rq.route_id = rt.id
  GROUP BY rq.line_number, rt.id, rt.line_ref)
INSERT INTO netex.netex_route_data (line_number, route_id, line_ref, direction_type, quay_list, stop_place_list, quay_count, start_quay_code, end_quay_code, start_stop_place_code, end_stop_place_code)
SELECT stats.*,
    start_quay.quay_code AS start_quay_code,
    end_quay.quay_code AS end_quay_code,
    start_quay.stop_place_code AS start_stop_place_code,
    end_quay.stop_place_code AS end_stop_place_code
FROM stats
JOIN netex.netex_route_quay start_quay ON start_quay.route_id = stats.route_id AND start_quay.quay_location_type = 'start'
JOIN netex.netex_route_quay end_quay ON end_quay.route_id = stats.route_id AND end_quay.quay_location_type = 'end';
""";

    private final static String update_netex_route_variant_sql = """
TRUNCATE TABLE netex.netex_route_variant;
INSERT INTO netex.netex_route_variant (line_number, direction_type, quay_list, stop_place_list, quay_count, start_quay_code, end_quay_code, start_stop_place_code, end_stop_place_code, line_ref, route_refs, colour, network)
SELECT nrd.line_number, nrd.direction_type, nrd.quay_list, nrd.stop_place_list, nrd.quay_count, 
    nrd.start_quay_code, nrd.end_quay_code, nrd.start_stop_place_code, nrd.end_stop_place_code, 
    nrd.line_ref, ARRAY_AGG(nrd.route_id) AS route_refs, nl.colour, nl.network
FROM netex.netex_route_data nrd
  LEFT JOIN netex.netex_line nl ON nl.id = nrd.line_ref
GROUP BY nrd.line_number, nrd.direction_type, nrd.quay_list, nrd.stop_place_list, nrd.quay_count, nrd.start_quay_code, nrd.end_quay_code, nrd.start_stop_place_code, nrd.end_stop_place_code, nrd.line_ref, nl.colour, nl.network
""";

    private final static String update_netex_route_variant_quay_sql = """
TRUNCATE TABLE netex.netex_route_variant_quay;
INSERT INTO netex.netex_route_variant_quay(
        line_number, variant_id, quay_code, stop_side_code, stop_place_code, quay_index, quay_location_type)
    SELECT nrq.line_number, variant.id, nrq.quay_code, nrq.stop_side_code, nrq.stop_place_code, nrq.quay_index, nrq.quay_location_type
    FROM netex.netex_route_variant variant
    JOIN netex.netex_route_quay nrq ON nrq.route_id = variant.route_refs[1]
""";
    
    private final static String update_netex_route_variant_data_sql = """
TRUNCATE TABLE netex.netex_route_variant_data;
INSERT INTO netex.netex_route_variant_data (line_number, variant_id, line_ref, direction_type, quay_list, stop_place_list, quay_count, start_quay_code, end_quay_code, start_stop_place_code, end_stop_place_code)
SELECT nrd.line_number, nrv.id, nrd.line_ref, nrd.direction_type, nrd.quay_list, nrd.stop_place_list, nrd.quay_count, nrd.start_quay_code, nrd.end_quay_code, nrd.start_stop_place_code, nrd.end_stop_place_code
FROM netex.netex_route_data nrd
JOIN netex.netex_route_variant nrv ON nrd.route_id = nrv.route_refs[1]
""";
    
    private final static String update_netex_line_endpoint_sql = """
TRUNCATE TABLE netex.netex_line_endpoint;
INSERT INTO netex.netex_line_endpoint
SELECT DISTINCT *
FROM (
      SELECT line.id AS netex_line_id, rd.line_number, rd.start_stop_place_code AS stop_place_code
      FROM netex.netex_line line
        JOIN netex.netex_route route ON route.line_ref = line.id
        JOIN netex.netex_route_data rd ON rd.route_id = route.id
      WHERE line.transport_mode = 'bus'
    UNION 
      SELECT line.id AS netex_line_id, rd.line_number, rd.end_stop_place_code AS stop_place_code
      FROM netex.netex_line line
        JOIN netex.netex_route route ON route.line_ref = line.id
        JOIN netex.netex_route_data rd ON rd.route_id = route.id
      WHERE line.transport_mode = 'bus'
    ) AS SUB
    WHERE stop_place_code IS NOT NULL;
""";

    private final static String update_netex_links_sql = """
TRUNCATE TABLE netex.netex_link;
INSERT INTO netex.netex_link
SELECT DISTINCT rq1.quay_code AS quay_code1, rq1.stop_side_code AS stop_side_code1, rq1.stop_place_code AS stop_place_code1,
    rq2.quay_code AS quay_code2, rq2.stop_side_code AS stop_side_code2, rq2.stop_place_code AS stop_place_code2
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
            .next(sqlUpdateStep("Update route variants", update_netex_route_variant_sql))
            .next(sqlUpdateStep("Update route variant quays", update_netex_route_variant_quay_sql))
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