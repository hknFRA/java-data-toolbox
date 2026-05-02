package spark;

import infrastructure.spark.SparkCore;
import org.apache.spark.sql.SparkSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SparkPgTest {

    private static final Logger log = LoggerFactory.getLogger(SparkPgTest.class);

    @Test
    void test_get_unit_test_spark_session() {
        log.info("hakan");

        // when
        SparkSession sparkSession = SparkCore.getUnitTestSparkSession();

        // then
        String version = sparkSession.version();
        Assertions.assertThat(version).isNotNull();
    }

}