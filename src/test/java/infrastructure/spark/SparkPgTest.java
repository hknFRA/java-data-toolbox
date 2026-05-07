package infrastructure.spark;

import com.holdenkarau.spark.testing.JavaDatasetSuiteBase;
import infrastructure.JdbcCore;
import io.zonky.test.db.postgres.embedded.FlywayPreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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

    JavaDatasetSuiteBase javaDatasetSuiteBase = new JavaDatasetSuiteBase();

    @Test
    void test_given_csv_should_be_write_to_db() {
        // given
        Dataset<Row> df = sparkSession.read().option("header", true).csv("src/test/resources/samples/s1.csv");

        // when
        SparkPg.write(df, url, id, pass, "public", "table_countries", SaveMode.Overwrite, 100);
        Dataset<Row> dfResult = SparkPg.readTable(sparkSession, url, id, pass, "public", "table_countries").df();

        // then
        javaDatasetSuiteBase.assertDatasetEquals(df, dfResult);
    }

}