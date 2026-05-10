package infrastructure.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkCassandra {
    // https://github.com/apache/cassandra-spark-connector/blob/trunk/doc/14_data_frames.md
    // https://github.com/apache/cassandra-spark-connector/blob/trunk/doc/reference.md

    public record Info(
            Dataset<Row> df,
            String keyspace,
            String table,
            String cql,
            long durationTimeMs,
            long triggerTimeMs) {
    }

    private static final Logger log = LoggerFactory.getLogger(SparkCassandra.class);

    public Info write(Dataset<Row> dataset, String url, String user, String pass,
                      String keyspace, String table, SaveMode saveMode) {

        dataset.write()
                .format("cassandra")
                // spark cassandra
                .option("keyspace", keyspace)
                .option("table", table)
                .option("spark.cassandra.connection.host", url)
                .option("spark.cassandra.auth.username", user)
                .option("spark.cassandra.auth.password", pass)

                .option("spark.cassandra.output.timestamp", "") // todo
                .option("spark.cassandra.output.ttl", "") // todo

                .mode(saveMode)
                .save();

        return new Info(null, keyspace, table, null, 0L, 0L);
    }

    /**
     *
     * */
    public Info read(SparkSession sparkSession, String url, String user, String pass,
                     String keyspace, String table) {

        sparkSession.read()
                .format("cassandra")
                // spark cassandra
                .option("keyspace", keyspace)
                .option("table", table)
                .option("spark.cassandra.connection.host", url)
                .option("spark.cassandra.auth.username", user)
                .option("spark.cassandra.auth.password", pass)
                .option("spark.cassandra.output.timestamp", "") // todo
                .option("spark.cassandra.output.ttl", "") // todo
                .load();

        return new Info(null, keyspace, table, null, 0L, 0L);
    }


}