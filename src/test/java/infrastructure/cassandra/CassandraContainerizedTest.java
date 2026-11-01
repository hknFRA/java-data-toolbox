package infrastructure.cassandra;

import com.holdenkarau.spark.testing.JavaDatasetSuiteBase;
import infrastructure.flyway.FlywayCore;
import infrastructure.spark.SparkCassandra;
import infrastructure.spark.SparkCore;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.cassandra.CassandraContainer;

import java.net.InetSocketAddress;

public class CassandraContainerizedTest {

    // container
    static CassandraContainer cassandraContainer = new CassandraContainer("cassandra:6.0");

    // spark
    SparkSession spark = SparkCore.getUnitTestSparkSession();
    JavaDatasetSuiteBase javaDatasetSuiteBase = new JavaDatasetSuiteBase();

    @Test
    void test_cassandra_containerized() {
        // given
        cassandraContainer.start();
        InetSocketAddress contactPoint = cassandraContainer.getContactPoint();
        String username = cassandraContainer.getUsername();
        String password = cassandraContainer.getPassword();
        String localDatacenter = cassandraContainer.getLocalDatacenter();
        String host = contactPoint.toString().replace("localhost/", "");
        Dataset<Row> df = spark.read().option("header", true).csv("src/test/resources/samples/s1.csv");
        String schema = "public";
        String table = "table_countries";

        // when
        FlywayCore.flywayInitTablesCassandraNative("jdbc:cassandra://" + host + "?localdatacenter=" + localDatacenter, username, password,
                "db-testing/cassandra/migration", "tab_audit_flyway_history");
        SparkCassandra.write(df, host, username, password, schema, table, SaveMode.Append);
        Dataset<Row> dfResult = SparkCassandra.read(spark, host, username, password, schema, table).df();
        long populationResult = (long) dfResult.filter("id = 1").selectExpr("population")
                .collectAsList()
                .getFirst()
                .get(0);

        // then
        Assertions.assertThat(populationResult).isEqualTo(100000L);
    }

}
