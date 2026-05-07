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
        SparkPg.write(df, url, id, pass, schema, table, SaveMode.Append, 100);

        // result
        Dataset<Row> dfResult = SparkPg.readTable(sparkSession, url, id, pass, schema, table).df();
        Map<String, @Nullable Object> count = namedParameterJdbcTemplate.getJdbcTemplate().queryForMap("select count(*) as c_ from %s.%s".formatted(schema, table));
        long sum = dfResult.selectExpr("sum(population) as s_")
                .collectAsList()
                .getFirst()
                .getLong(0);

        // then
        Assertions.assertThat(count.get("c_")).isEqualTo(3L);
        Assertions.assertThat(dfResult.count()).isEqualTo(3L);
        Assertions.assertThat(sum).isEqualTo(330000L);
    }

}