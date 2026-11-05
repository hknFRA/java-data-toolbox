package infrastructure.spark;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.UUID;

/**
 * https://spark.apache.org/docs/latest/streaming/structured-streaming-kafka-integration.html
 * spark kafka 2 methods
 * -> batch
 * -> streaming
 */
public class SparkKafka {

    public static Dataset<Row> readBatch(SparkSession spark, String topic, String servers) {
        return spark
                .read()
                .format("kafka")
                // kafka
                .option("kafka.bootstrap.servers", servers)
                .option("kafka" + ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString())
                .option("kafka" + ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                //.option(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class.getName())
                //.option(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class.getName())
                // spark
                .option("subscribe", topic)
                .load()
                .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)");
    }


}
