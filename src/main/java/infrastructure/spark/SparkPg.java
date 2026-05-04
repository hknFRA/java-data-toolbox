package infrastructure.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

public class SparkPg {

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
        dataset.write()
                // spark options : https://spark.apache.org/docs/latest/sql-data-sources-jdbc.html
                .format("jdbc")
                .option("url", url)
                .option("user", user)
                .option("password", pass)
                .option("driver", "org.postgresql")
                .option("dbtable", table)
                .option("batchsize", batchSize)

                // jdbc custom tuning : https://jdbc.postgresql.org/documentation/use/
                .option("currentSchema", schema)
                .option("ApplicationName", "spark-java-writer")
                .option("stringtype", "unspecified")
                .option("reWriteBatchedInserts", true)
                .mode(saveMode);
    }


    /**
     *
     */
    public static Info readTable(SparkSession sparkSession, String url, String user, String pass,
                                 String schema, String table) {

        return new Info(null, 0L);
    }

    /**
     *
     */
    public static Info readSql(SparkSession sparkSession, String url, String user, String pass, String sql) {
        return new Info(null, 0L);
    }
}
