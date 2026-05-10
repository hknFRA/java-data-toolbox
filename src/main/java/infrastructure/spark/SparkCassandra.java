package infrastructure.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;

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

    public Info write(Dataset<Row> dataset, String url, String user, String pass,
                      String keyspace, String table, SaveMode saveMode) {

        dataset.write()
                .format("cassandra")
                .option("keyspace", keyspace)
                .option("table", table)
                .mode(saveMode)
                .save();

        return new Info(null, keyspace, table, null, 0L, 0L);
    }

}