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
import org.apache.spark.sql.types.StructType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.*;

class SparkCertificationTrainingTest {

    @RegisterExtension
    public static PreparedDbExtension db =
            EmbeddedPostgresExtension.preparedDatabase(
                    FlywayPreparer.forClasspathLocation("db-testing/postgres/migration"));

    JdbcSecrets secrets = JdbcSecrets.getJdbcSecrets(db.getTestDatabase());
    String url = secrets.url();
    String id = secrets.id();
    String pass = "";

    SparkSession spark = SparkCore.getUnitTestSparkSession();

    NamedParameterJdbcTemplate namedParameterJdbcTemplate = JdbcTemplateClients.getNamedParameterJdbcTemplate(url, id, pass, PgCore.ORG_POSTGRESQL_DRIVER);

    JavaDatasetSuiteBase javaDatasetSuiteBase = new JavaDatasetSuiteBase();


    @Test
    void test_given_csv_should_be_transform_and_write_to_db_and_query_sql() {
        // given
        Dataset<Row> dfIn = spark.read().option("header", true).csv("src/test/resources/samples/s1.csv")
                .withColumn("population", functions.col("population").cast(DataTypes.LongType));
        dfIn.createOrReplaceTempView("df_in");
        dfIn.cache();

        Dataset<Row> dfInUnderscores = spark.read().option("header", true).csv("src/test/resources/samples/s5_underscores.csv")
                .withColumn("population", functions.col("population").cast(DataTypes.LongType))
                .withColumn("is_valid", functions.col("is_valid").try_cast(DataTypes.BooleanType));
        dfInUnderscores.cache();

        Dataset<Row> dfInUnderscoresDuplicates = spark.read().option("header", true).csv("src/test/resources/samples/s5_underscores.csv")
                .withColumn("population", functions.col("population").cast(DataTypes.LongType))
                .withColumn("is_valid", functions.col("is_valid").try_cast(DataTypes.BooleanType));
        dfInUnderscoresDuplicates.cache();

        Dataset<Row> dfInUnderscoresDuplicates3Columns = spark.read().option("header", true).csv("src/test/resources/samples/s7_underscores_duplicates_on_4_columns.csv")
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
            spark.udf().register("udf_plus_x", udf1, DataTypes.IntegerType);
            Dataset<Row> df = spark.sql("SELECT udf_plus_x(5)");
        }

        // 4. udf with associated column
        {
            // udf must be invoked with spark.sql() api (not compatible with df.select())
            // udf can refer to a regular java object who is Serializable
            // spark catalyst cannot optimize udf function

            Random random = new Random();
            UDF1<Long, Long> udf1 = columnValue -> columnValue + random.nextLong();
            spark.udf().register("udf_column_plus_random", udf1, DataTypes.LongType);
            //Dataset<Row> df = dfIn.selectExpr("select *, udf_column_plus_random(population) as random_population");
            Dataset<Row> df = spark.sql("select *, udf_column_plus_random(population) as random_population from df_in");
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


            // create dataframe
            List<Row> rows = List.of(
                    RowFactory.create("123", "hakan", 26),
                    RowFactory.create("456", "hanim", 29)
            );
            StructType structType = new StructType();
            structType = structType.add("id", DataTypes.StringType, false);
            structType = structType.add("name", DataTypes.StringType, false);
            structType = structType.add("age", DataTypes.IntegerType, false);
            Dataset<Row> df11 = spark.createDataFrame(rows, structType);
            df11.orderBy("age");

        }

    }

    @Test
    void test_structure_schema_on_csv() {
        // IMPORTANT
        // struct type declare column in exact order
        // if schema column number 3 is "is_capital_city", it will be mapped on csv column 3 even if it's not corresponding column

        // IMPORTANT
        // if schema define more columns than input csv, extra columns will be NULL

        StructType structType = new StructType();
        structType = structType.add("id", DataTypes.StringType, false);
        structType = structType.add("pays", DataTypes.StringType, false);
        structType = structType.add("is_capital_city", DataTypes.StringType, false);
        Dataset<Row> dfIn = spark.read()
                .schema(structType)
                .option("header", true)
                .csv("src/test/resources/samples/s1.csv");
        dfIn.show();
    }

    /// [1](https://spark.apache.org/docs/latest/sql-data-sources-csv.html)
    /// [2](https://docs.databricks.com/aws/en/query/formats/csv?language=Scala#handle-malformed-csv-records)
    ///
    /// force lazy evaluation on csv data
    /// count() : keep initial size of df
    /// collectAsList() : force evaluation and with exact size
    @Test
    void test_read_csv() {

        {
            /// PERMISSIVE (default)
            StructType structType = new StructType();
            structType = structType.add("id", DataTypes.StringType, false);
            structType = structType.add("pays", DataTypes.StringType, false);
            Dataset<Row> dfIn = spark.read().schema(structType)
                    .option("header", true).option("mode", "PERMISSIVE").csv("src/test/resources/samples/s9_two_columns.csv");
            Assertions.assertThat(dfIn.count()).isEqualTo(3L);
        }

        {
            /// FAILFAST (column OK)
            StructType structType = new StructType();
            structType = structType.add("id", DataTypes.StringType, false);
            Dataset<Row> dfIn = spark.read().schema(structType)
                    .option("header", true).option("mode", "FAILFAST").csv("src/test/resources/samples/s9_two_columns.csv");
            Assertions.assertThat(dfIn.count()).isEqualTo(3L);
        }

        {
            /// FAILFAST (1 column OK, 1 column KO not exists)
            StructType structType = new StructType();
            structType = structType.add("id", DataTypes.StringType, false);
            structType = structType.add("new_column", DataTypes.LongType, false);
            Exception e = null;
            try {
                Dataset<Row> dfIn = spark.read().schema(structType)
                        .option("header", true).option("mode", "FAILFAST").csv("src/test/resources/samples/s9_two_columns.csv");
                dfIn.collect(); // lazy init eval
            } catch (Exception eTmp) {
                e = eTmp;
            }
            Assertions.assertThat(e).isNotNull();
        }

        {
            /// DROPMALFORMED (1 column OK, 1 column KO bad type)
            StructType structType = new StructType();
            structType = structType.add("id", DataTypes.StringType, false);
            structType = structType.add("ville", DataTypes.LongType, false);
            Dataset<Row> dfIn = spark.read().schema(structType)
                    .option("header", true).option("mode", "DROPMALFORMED").csv("src/test/resources/samples/s9_two_columns.csv");
            Assertions.assertThat(dfIn.collectAsList().size()).isEqualTo(0);
        }

    }


    @Test
    void test_range_and_create_view() {
        // range : inclusive -> exclusive
        // implicit column 'id'
        Dataset<Long> dfRange = spark.range(1, 101);

        // createOrReplaceTempView
        // https://stackoverflow.com/questions/42774187/spark-createorreplacetempview-vs-createglobaltempview
        dfRange.createOrReplaceTempView("tmp_view");
        Assertions.assertThat(spark.sql("select * from tmp_view").count()).isEqualTo(100);

        // createOrReplaceGlobalTempView
        // global view will be put in default schema --> global_temp
        dfRange.createOrReplaceGlobalTempView("view");
        Assertions.assertThat(spark.sql("select * from global_temp.view").count()).isEqualTo(100);
    }

    @Test
    void test_time_function() {
        // date_format() -> timestamp to date string
        // from_unixtime() -> epoch second long to date string
        // to_timestamp() -> epoch second long to timestamp

        // show() on timestamp type human-readable string, but internally it's timestamp

        Dataset<Row> dfIn = spark.read().option("header", true).csv("src/test/resources/samples/s8_timestamp.csv")
                .withColumn("time_ms", functions.col("time_ms").cast(DataTypes.LongType))
                .withColumn("time_s", functions.col("time_ms").divide(1000).cast(DataTypes.LongType))
                .withColumn("time_s_as_timestamp", functions.col("time_s").cast(DataTypes.TimestampType));

        String format = "yyyy-MM-dd";
        Dataset<Row> df = dfIn
                .withColumn("date", functions.to_timestamp(functions.col("time_s")))
                .withColumn("date_format", functions.date_format(functions.col("time_s_as_timestamp"), format))
                .withColumn("date_format_2", functions.from_unixtime(functions.col("time_s"), format));
        df.printSchema();
        df.show();
    }

    @Test
    void test_time_function_and_create_view() {
        Dataset<Row> dfIn = spark.read().option("header", true).csv("src/test/resources/samples/s1.csv")
                .withColumn("population", functions.col("population").cast(DataTypes.LongType));

        dfIn.write().format("parquet");

        //dfIn.withColumn("f1", functions.avg())
        /* TODO
        df.write.format("parquet") \
    .sortBy("customer_id") \
    .saveAsTable("sales.customers_sorted")
         */

    }

    //@Test
    //void test_repartition() {
    //    // https://stackoverflow.com/questions/42171499/get-current-number-of-partitions-of-a-dataframe
//
    //    Dataset<Long> dfIn = spark.range(0, 1000, 1, 6);
    //    System.out.println(dfIn.rdd().getNumPartitions());
//
    //    dfIn.mapPartitions(input -> {
    //
    //    })
//
    //    Dataset<Long> dfRepartition = dfIn.repartition(10);
    //    System.out.println(dfRepartition.rdd().getNumPartitions());
    //    System.out.println(Arrays.toString(dfRepartition.rdd().getPartitions()));
    //}


    @Test
    void test_add_column_from_sql_query() {
        // prepare db
        Dataset<Row> df = spark.read().option("header", true).csv("src/test/resources/samples/s1.csv");
        SparkPg.write(df, secrets.url(), secrets.id(), pass, "public", "table_countries", SaveMode.Overwrite, 1000);

        // given
        Dataset<Row> dfIn = spark.read().option("header", true).csv("src/test/resources/samples/s9_two_columns.csv")
                .withColumn("query", functions.concat(
                        functions.lit("select pays, population from public.table_countries where id = '"), functions.lit(functions.col("id")), functions.lit("'"),
                        functions.lit(" and ville = '"), functions.lit(functions.col("ville")), functions.lit("'")
                ));

        // when
        Dataset<Row> dfFinal = SparkPg.getDfUpdatedWithJdbcCall(dfIn, df, namedParameterJdbcTemplate, "query");

        // then
        Assertions.assertThat(dfFinal.columns().length).isEqualTo(5);

    }


}