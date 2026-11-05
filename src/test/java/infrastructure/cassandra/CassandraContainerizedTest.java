package infrastructure.cassandra;

import infrastructure.flyway.FlywayCore;
import infrastructure.spark.SparkCassandra;
import infrastructure.spark.SparkCore;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.cassandra.CassandraContainer;

import java.net.InetSocketAddress;

public class CassandraContainerizedTest {

    // container
    static CassandraContainer cassandraContainer = new CassandraContainer("cassandra:6.0");
    static String host;
    static String username;
    static String password;
    static String localDatacenter;

    // spark
    SparkSession spark = SparkCore.getUnitTestSparkSession();

    @BeforeAll
    static void setup() {
        cassandraContainer.start();
        InetSocketAddress contactPoint = cassandraContainer.getContactPoint();
        username = cassandraContainer.getUsername();
        password = cassandraContainer.getPassword();
        localDatacenter = cassandraContainer.getLocalDatacenter();
        host = contactPoint.toString().replace("localhost/", "");
        FlywayCore.flywayInitTablesCassandraNative("jdbc:cassandra://" + host + "?localdatacenter=" + localDatacenter, username, password,
                "db-testing/cassandra/migration", "tab_audit_flyway_history");
    }

    @Test
    void test_cassandra_containerized() {
        // given
        String schema = "public";
        String table = "table_countries";
        Dataset<Row> df = spark.read().option("header", true).csv("src/test/resources/samples/s1.csv");

        // when
        SparkCassandra.write(df, host, username, password, schema, table, SaveMode.Append);
        Dataset<Row> dfResult = SparkCassandra.read(spark, host, username, password, schema, table).df();
        long populationResult = (long) dfResult
                .filter("id = 1")
                .selectExpr("population")
                .collectAsList()
                .getFirst()
                .get(0);

        // then
        Assertions.assertThat(populationResult).isEqualTo(100000L);
    }

}
