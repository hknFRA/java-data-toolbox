package infrastructure.spark;

import infrastructure.postgres.PgCore;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkPg {


    private static final Logger log = LoggerFactory.getLogger(SparkPg.class);

    public record Info(
            Dataset<Row> df,
            long timeMs
    ) {
    }

    /**
     * write spark dataframe to postgres table
     */
    public static void write(Dataset<Row> dataset, String url, String user, String pass,
                             String schema, String table, SaveMode saveMode, int batchSize) {

        String tableFinal = schema == null || schema.isEmpty() ?
                table : String.join(".", schema, table);

        dataset.write()
                // spark options : https://spark.apache.org/docs/latest/sql-data-sources-jdbc.html
                .format("jdbc")
                .option("url", url)
                .option("user", user)
                .option("password", pass)
                .option("driver", PgCore.ORG_POSTGRESQL_DRIVER)
                .option("dbtable", tableFinal)
                .option("batchsize", batchSize)

                // jdbc custom tuning : https://jdbc.postgresql.org/documentation/use/
                .option("currentSchema", schema)
                .option("ApplicationName", "spark-java-writer")
                .option("stringtype", "unspecified")
                .option("reWriteBatchedInserts", true)
                .mode(saveMode)
                .save();
    }


    /**
     *
     */
    public static Info readTable(SparkSession sparkSession, String url, String user, String pass,
                                 String schema, String table) {

        long start = System.currentTimeMillis();
        log.info("start read spark jdbc. url : {} , schema : {} , table : {} , user : {}",
                url, schema, table, user);
        String tableFinal = schema == null || schema.isEmpty() ?
                table : String.join(".", schema, table);

        Dataset<Row> df = sparkSession.read()
                .format("jdbc")
                // spark
                .option("url", url)
                .option("user", user)
                .option("password", pass)
                .option("driver", PgCore.ORG_POSTGRESQL_DRIVER)
                .option("dbtable", tableFinal)
                // jdbc
                .option("ApplicationName", "spark-java-reader")
                .load();
        df.printSchema();

        long end = System.currentTimeMillis() - start;
        return new Info(df, end);
    }

    /**
     *
     */
    public static Info readSql(SparkSession sparkSession, String url, String user, String pass, String sql) {
        return new Info(null, 0L);
    }
}
