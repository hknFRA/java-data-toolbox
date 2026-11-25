package infrastructure.spark;

import com.holdenkarau.spark.testing.JavaDatasetSuiteBase;
import infrastructure.jdbc.JdbcSecrets;
import infrastructure.jdbc.JdbcTemplateClients;
import infrastructure.postgres.PgCore;
import io.zonky.test.db.postgres.embedded.FlywayPreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import org.apache.commons.io.FileUtils;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import tools.logger.Slf4jWithLogback;

import java.io.File;
import java.util.Map;

class SparkCertificationHiveTrainingTest {


    @RegisterExtension
    public static PreparedDbExtension db =
            EmbeddedPostgresExtension.preparedDatabase(
                    FlywayPreparer.forClasspathLocation("db-testing/postgres/migration"));

    JdbcSecrets secrets = JdbcSecrets.getJdbcSecrets(db.getTestDatabase());
    String url = secrets.url();
    String id = secrets.id();
    String pass = "";

    SparkSession spark = SparkCore.getUnitTestSparkSessionWithHive();

    NamedParameterJdbcTemplate namedParameterJdbcTemplate = JdbcTemplateClients.getNamedParameterJdbcTemplate(url, id, pass, PgCore.ORG_POSTGRESQL_DRIVER);

    JavaDatasetSuiteBase javaDatasetSuiteBase = new JavaDatasetSuiteBase();

    @BeforeEach
    void beforeEach() {
        Slf4jWithLogback.changeLevel(Map.of(
                "org.apache.spark", "OFF",
                "org.apache.parquet.hadoop", "OFF",
                "org.apache.hadoop.mapreduce", "OFF",
                "org.apache.parquet", "OFF"
        ));
        FileUtils.deleteQuietly(new File("./work"));
        Dataset<Row> df = spark.read().option("header", true).csv("src/test/resources/samples/s1.csv");
        SparkPg.write(df, url, id, pass, "public", "table_countries", SaveMode.Overwrite, 10);
    }


    //@Test
    //void test_spark_sql() throws NoSuchTableException {
//
    //    // given
    //    Dataset<Row> df1 = spark.read().option("header", true).csv("src/test/resources/samples/s1.csv")
    //            .withColumn("population", functions.col("population").cast(DataTypes.LongType));
//
    //    // when
//
    //    df1.write().saveAsTable();
    //    df1.write().insertInto();
    //    df1.writeTo();
    //    Dataset<Row> table = spark.read().table("spark_schema.table_1");
    //    table.show();
    //}


    //@Test
    //void test_spark_write_bucketBy() {
    //    spark.catalog().createTable
    //}

    @Test
    void test_spark_write_insertInto() {
        // given
        Dataset<Row> df1 = spark.read().option("header", true).csv("src/test/resources/samples/s1.csv")
                .withColumn("population", functions.col("population").cast(DataTypes.LongType));
        String tableFqn = "my_table";

        // when
        df1.write().mode(SaveMode.Overwrite).saveAsTable(tableFqn);

        Dataset<Row> table = spark.read().table(tableFqn);

        System.out.println(spark.catalog().currentCatalog());
        System.out.println(spark.catalog().currentDatabase());

        df1.createOrReplaceTempView("tmp_view");
        spark.catalog().listCatalogs().show(false);
        spark.catalog().listDatabases().show(false);
        spark.catalog().listTables().show(false);


        //df1.write().insertInto();
        //df1.writeTo();


        // hierarchy : catalog.schema.table
        // native catalog.schemas
        // spark_schema.default
        // global_temp

        table.show();
    }

}