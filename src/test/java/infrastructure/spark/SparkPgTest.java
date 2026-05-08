package infrastructure.spark;

import com.holdenkarau.spark.testing.JavaDatasetSuiteBase;
import infrastructure.JdbcCore;
import infrastructure.postgres.PgCore;
import io.zonky.test.db.postgres.embedded.FlywayPreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;
import java.util.Properties;

class SparkPgTest {

    @RegisterExtension
    public static PreparedDbExtension db =
            EmbeddedPostgresExtension.preparedDatabase(
                    FlywayPreparer.forClasspathLocation("db-testing/postgres/migration"));

    JdbcCore.JdbcSecrets secrets = JdbcCore.JdbcSecrets.getJdbcSecrets(db.getTestDatabase());
    String url = secrets.url();
    String id = secrets.id();
    String pass = "";

    SparkSession sparkSession = SparkCore.getUnitTestSparkSession();

    NamedParameterJdbcTemplate namedParameterJdbcTemplate = PgCore.getNamedParameterJdbcTemplate(url, id, pass);

    JavaDatasetSuiteBase javaDatasetSuiteBase = new JavaDatasetSuiteBase();

    @Test
    void test_given_csv_should_be_write_to_db() {
        // given
        String schema = "public";
        String table = "table_countries";
        Dataset<Row> df = sparkSession.read().option("header", true).csv("src/test/resources/samples/s1.csv");

        // when
        SparkPg.write(df, url, id, pass, schema, table, SaveMode.Overwrite, 100);

        // result
        SparkPg.Info info = SparkPg.readTable(sparkSession, url, id, pass, schema, table);
        Dataset<Row> dfResult = info.df();
        Map<String, @Nullable Object> count = namedParameterJdbcTemplate.getJdbcTemplate().queryForMap("select count(*) as c_ from %s.%s".formatted(schema, table));
        // spark sql cast fix error of type java.lang.ClassCastException: class java.lang.Double cannot be cast to class java.lang.Long
        Object sum = dfResult.selectExpr("cast(sum(population) as long) as s_")
                .collectAsList()
                .getFirst()
                .get(0);

        // then
        Assertions.assertThat(count.get("c_")).isEqualTo(3L);
        Assertions.assertThat(dfResult.count()).isEqualTo(3L);
        Assertions.assertThat(sum).isEqualTo(330000L);
        Assertions.assertThat(info.schema()).isEqualTo(schema);
        Assertions.assertThat(info.table()).isEqualTo(table);
        Assertions.assertThat(info.durationTimeMs()).isGreaterThan(0L);
        Assertions.assertThat(info.triggerTimeMs()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void test_given_csv_should_be_write_to_db_with_props() {
        // given
        String schema = "public";
        String table = "table_countries";
        Dataset<Row> df = sparkSession.read().option("header", true).csv("src/test/resources/samples/s1.csv");
        Properties sparkProps = new Properties();
        sparkProps.put("url", url);
        sparkProps.put("user", id);
        sparkProps.put("password", pass);
        sparkProps.put("driver", PgCore.ORG_POSTGRESQL_DRIVER);
        sparkProps.put("batchsize", "1000");
        sparkProps.put("ApplicationName", "spark-java-writer");
        sparkProps.put("stringtype", "unspecified");
        sparkProps.put("reWriteBatchedInserts", "true");

        Properties pgProps = new Properties();

        // when
        SparkPg.write(df, schema, table, sparkProps, pgProps, SaveMode.Overwrite);

        // result
        Dataset<Row> dfResult = SparkPg.readTable(sparkSession, url, id, pass, schema, table).df();
        Map<String, @Nullable Object> count = namedParameterJdbcTemplate.getJdbcTemplate().queryForMap("select count(*) as c_ from %s.%s".formatted(schema, table));
        // spark sql cast fix error of type java.lang.ClassCastException: class java.lang.Double cannot be cast to class java.lang.Long
        Object sum = dfResult.selectExpr("cast(sum(population) as long) as s_")
                .collectAsList()
                .getFirst()
                .get(0);

        // then
        Assertions.assertThat(count.get("c_")).isEqualTo(3L);
        Assertions.assertThat(dfResult.count()).isEqualTo(3L);
        Assertions.assertThat(sum).isEqualTo(330000L);
    }

    @Test
    void test_given_csv_should_be_write_to_db_and_query_sql() {
        // given
        String schema = "public";
        String table = "table_countries";
        Dataset<Row> df = sparkSession.read().option("header", true).csv("src/test/resources/samples/s1.csv");

        // when
        SparkPg.write(df, url, id, pass, schema, table, SaveMode.Overwrite, 100);

        // result
        //1
        Dataset<Row> dfResult = SparkPg.readTable(sparkSession, url, id, pass, schema, table).df();
        // 2
        String sql = "select * from %s.%s".formatted(schema, table);
        Dataset<Row> dfResultSql = SparkPg.readSql(sparkSession, url, id, pass, sql).df();

        // then
        javaDatasetSuiteBase.assertDatasetEquals(dfResult, dfResultSql);
    }

}