package infrastructure.spark;

import infrastructure.postgres.PgCore;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.postgresql.PGProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SparkPg {


    private static final Logger log = LoggerFactory.getLogger(SparkPg.class);

    public record Info(
            Dataset<Row> df,
            String schema,
            String table,
            String sql,
            long durationTimeMs,
            long triggerTimeMs) {
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
                .option(PGProperty.CURRENT_SCHEMA.getName(), schema)
                .option(PGProperty.APPLICATION_NAME.getName(), "spark-java-writer")
                .option(PGProperty.STRING_TYPE.getName(), "unspecified")
                .option(PGProperty.REWRITE_BATCHED_INSERTS.getName(), true)
                .mode(saveMode)
                .save();
    }

    /**
     *
     */
    public static void write(Dataset<Row> dataset, String schema, String table,
                             Properties sparkProps, Properties pgProps, SaveMode saveMode) {
        // todo : check if spark props are OK
        // todo : check if jdbc props are OK
        // todo : convert all props values to string (cast error)

        String url = sparkProps.getProperty("url");
        String tableFinal = schema == null || schema.isEmpty() ?
                table : String.join(".", schema, table);

        dataset.write()
                .mode(saveMode)
                .jdbc(url, tableFinal, sparkProps);

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
                .option(PGProperty.APPLICATION_NAME.getName(), "spark-java-reader")
                .load();
        df.printSchema();

        long time = System.currentTimeMillis() - start;
        return new Info(df, schema, table, null, time, start);
    }

    /**
     *
     */
    public static Info readSql(SparkSession sparkSession, String url, String user, String pass, String sql) {

        long start = System.currentTimeMillis();
        log.info("start read spark jdbc. url : {} , user : {}, sql : {}",
                url, user, sql);

        Dataset<Row> df = sparkSession.read()
                .format("jdbc")
                // spark
                .option("url", url)
                .option("user", user)
                .option("password", pass)
                .option("driver", PgCore.ORG_POSTGRESQL_DRIVER)
                .option("query", sql)
                // jdbc
                .option(PGProperty.APPLICATION_NAME.getName(), "spark-java-reader")
                .load();
        df.printSchema();

        long time = System.currentTimeMillis() - start;
        return new Info(df, null, null, sql, time, start);
    }
}
