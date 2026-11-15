package infrastructure.spark;

import com.holdenkarau.spark.testing.JavaDatasetSuiteBase;
import infrastructure.jdbc.JdbcSecrets;
import infrastructure.jdbc.JdbcTemplateClients;
import infrastructure.postgres.PgCore;
import io.zonky.test.db.postgres.embedded.FlywayPreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import org.apache.spark.sql.*;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.types.DataTypes;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;
import java.util.Properties;
import java.util.Random;

class SparkPgTest {

    @RegisterExtension
    public static PreparedDbExtension db =
            EmbeddedPostgresExtension.preparedDatabase(
                    FlywayPreparer.forClasspathLocation("db-testing/postgres/migration"));

    JdbcSecrets secrets = JdbcSecrets.getJdbcSecrets(db.getTestDatabase());
    String url = secrets.url();
    String id = secrets.id();
    String pass = "";

    SparkSession sparkSession = SparkCore.getUnitTestSparkSession();

    NamedParameterJdbcTemplate namedParameterJdbcTemplate = JdbcTemplateClients.getNamedParameterJdbcTemplate(url, id, pass, PgCore.ORG_POSTGRESQL_DRIVER);

    JavaDatasetSuiteBase javaDatasetSuiteBase = new JavaDatasetSuiteBase();

    @Test
    void test_given_csv_should_be_write_to_db() {
        // given
        String schema = "public";
        String table = "table_countries";
        Dataset<Row> df = sparkSession.read().option("header", true).csv("src/test/resources/samples/s1.csv");
        long start = System.currentTimeMillis();

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
        Assertions.assertThat(info.triggerTimeMs()).isGreaterThan(start);
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

    @Test
    void test_given_csv_should_be_transform_and_write_to_db_and_query_sql() {
        // given
        Dataset<Row> dfIn = sparkSession.read().option("header", true).csv("src/test/resources/samples/s1.csv")
                .withColumn("population", functions.col("population").cast(DataTypes.LongType));
        dfIn.createOrReplaceTempView("df_in");
        dfIn.cache();

        Dataset<Row> dfInUnderscores = sparkSession.read().option("header", true).csv("src/test/resources/samples/s5_underscores.csv")
                .withColumn("population", functions.col("population").cast(DataTypes.LongType))
                .withColumn("is_valid", functions.col("is_valid").try_cast(DataTypes.BooleanType));
        dfInUnderscores.cache();

        Dataset<Row> dfInUnderscoresDuplicates = sparkSession.read().option("header", true).csv("src/test/resources/samples/s5_underscores.csv")
                .withColumn("population", functions.col("population").cast(DataTypes.LongType))
                .withColumn("is_valid", functions.col("is_valid").try_cast(DataTypes.BooleanType));
        dfInUnderscoresDuplicates.cache();

        Dataset<Row> dfInUnderscoresDuplicates3Columns = sparkSession.read().option("header", true).csv("src/test/resources/samples/s7_underscores_duplicates_on_4_columns.csv")
                .withColumn("population", functions.col("population").cast(DataTypes.LongType))
                .withColumn("is_valid", functions.col("is_valid").try_cast(DataTypes.BooleanType));
        dfInUnderscoresDuplicates3Columns.cache();


        // 1. order desc
        {
            Dataset<Row> df = dfIn
                    .orderBy(functions.col("population").desc())
                    .selectExpr("population")
                    .limit(1);
            long r1 = Long.parseLong(SparkCore.getColumnValue(df, "population").toString());
            Assertions.assertThat(r1).isEqualTo(200000L);
        }


        // 2. count / approx
        {
            Dataset<Row> df2 = dfIn
                    .select(functions.approx_count_distinct("id").as("_approx_count"),
                            functions.count("id").as("_count"));
        }

        // 3. udf
        {
            // https://spark.apache.org/docs/latest/sql-ref-functions.html
            // https://spark.apache.org/docs/latest/sql-ref-functions-udf-scalar.html
            UDF1<Integer, Integer> udf1 = x -> {
                int base = 1000;
                return x + base;
            };
            sparkSession.udf().register("udf_plus_x", udf1, DataTypes.IntegerType);
            Dataset<Row> df = sparkSession.sql("SELECT udf_plus_x(5)");
        }

        // 4. udf with associated column
        {
            // udf must be invoked with spark.sql() api (not compatible with df.select())
            // udf can refer to a regular java object who is Serializable
            // spark catalyst cannot optimize udf function

            Random random = new Random();
            UDF1<Long, Long> udf1 = columnValue -> columnValue + random.nextLong();
            sparkSession.udf().register("udf_column_plus_random", udf1, DataTypes.LongType);
            //Dataset<Row> df = dfIn.selectExpr("select *, udf_column_plus_random(population) as random_population");
            Dataset<Row> df = sparkSession.sql("select *, udf_column_plus_random(population) as random_population from df_in");
            df.show();
        }

        // 5. filter / drop
        {
            Dataset<Row> df = dfIn.drop("population", "pays", "ville");
            Assertions.assertThat(df.columns()).hasSize(1);

            // python : filter allow operator like | , &
            Dataset<Row> df2 = dfIn.filter(functions.col("population").$greater$eq(200000));
            Assertions.assertThat(df2.count()).isEqualTo(1);

            Dataset<Row> df3 = dfIn.withColumn("multiply", functions.col("population").multiply(300));
            df3.show();

            Dataset<Row> df4 = dfIn.withColumn("constant", functions.lit("CONSTANT"));
            df4.show();

            // split() + element_at()
            // python : allow split(col("xxxx"), "_")[0] (0 based)
            // java : split + element_at(col("xxxx"), 1) (1 based)
            Dataset<Row> df5 = dfInUnderscores
                    .withColumn("splitTmp", functions.split(functions.col("year_month_report"), "_"))
                    .withColumn("year", functions.element_at(functions.col("splitTmp"), 1))
                    .withColumn("month", functions.element_at(functions.col("splitTmp"), 2))
                    .drop("splitTmp");
            df5.printSchema();
            df5.show();

            // explode
            Dataset<Row> df6 = dfInUnderscores
                    .withColumn("splitTmp", functions.split(functions.col("year_month_report"), "_"))
                    .withColumn("split_explode", functions.explode(functions.col("splitTmp")));
            df6.show();
            Assertions.assertThat(df6.count()).isEqualTo(6L);

            // regexp_replace
            String regex = "^\\d+_";
            Dataset<Row> df7 = dfInUnderscores
                    .withColumn("cleaned", functions.regexp_replace(functions.col("year_month_report"), regex, ""));
            df7.show();

            // withColumnRenamed
            Dataset<Row> df8 = dfIn
                    .withColumnRenamed("population", "population_renamed");
            df8.show();
            Assertions.assertThat(df8.columns()).doesNotContain("population");

            // na : return a NaFunction then trigger clean function (drop, fill) on df
            Dataset<Row> df9 = dfInUnderscores.na().drop();
            Assertions.assertThat(df9.count()).isEqualTo(2L);
            Dataset<Row> df10 = dfInUnderscores.na().fill(false);
            Assertions.assertThat(df10.filter("is_valid = false").count()).isEqualTo(2L);

            // dropDuplicates
            // python : drop_duplicates()
            // can be applied on subset
            Assertions.assertThat(dfInUnderscoresDuplicates.distinct().count()).isEqualTo(3L);
            Assertions.assertThat(dfInUnderscoresDuplicates.distinct().count()).isEqualTo(3L);
            Assertions.assertThat(dfInUnderscoresDuplicates3Columns.dropDuplicates(new String[]{"id"}).count()).isEqualTo(3L);
            Assertions.assertThat(dfInUnderscoresDuplicates3Columns.dropDuplicates(new String[]{"id", "pays", "ville", "population", "year_month_report", "is_valid"}).count()).isEqualTo(4L);
        }

    }

}