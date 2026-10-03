package infrastructure;

import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class KafkaCore {

    private static final Logger log = LoggerFactory.getLogger(KafkaCore.class);

    /**
     *
     * @param kafkaConsumer
     * @param millisTimeout
     * @param function
     * @param <K> kafka event key type
     * @param <V> kafka event value type
     */
    static public <K, V> void consume(KafkaConsumer<K, V> kafkaConsumer,
                                      long millisTimeout,
                                      Function<ConsumerRecord<K, V>, Boolean> function) {

        // client logs
        ConsumerGroupMetadata consumerGroupMetadata = kafkaConsumer.groupMetadata();
        MDC.put("mdc_kafka_consumer_group", consumerGroupMetadata.groupId());
        MDC.put("mdc_kafka_consumer_group_id", String.valueOf(consumerGroupMetadata.groupInstanceId()));
        MDC.put("mdc_kafka_consumer_member_id", consumerGroupMetadata.memberId());

        ConsumerRecords<K, V> records = kafkaConsumer.poll(Duration.ofMillis(millisTimeout));
        records.forEach(kvConsumerRecord -> {
            // record logs
            MDC.put("mdc_kafka_consumer_topic", kvConsumerRecord.topic());
            MDC.put("mdc_kafka_consumer_partition", String.valueOf(kvConsumerRecord.partition()));
            MDC.put("mdc_kafka_consumer_offset", String.valueOf(kvConsumerRecord.offset()));

            // call
            function.apply(kvConsumerRecord);
        });

        // commit when all messages are correctly processed
        kafkaConsumer.commitSync();
    }


    /**
     * blocking
     *
     * @param kafkaProducer
     * @param producerRecord
     * @param <K>
     * @param <V>
     * @throws ExecutionException
     * @throws InterruptedException
     */
    static public <K, V> void produce(KafkaProducer<K, V> kafkaProducer,
                                      ProducerRecord<K, V> producerRecord) throws ExecutionException, InterruptedException {

        RecordMetadata recordMetadata = kafkaProducer.send(producerRecord).get();

        MDC.put("mdc_kafka_producer_topic", recordMetadata.topic());
        MDC.put("mdc_kafka_producer_partition", String.valueOf(recordMetadata.partition()));
        MDC.put("mdc_kafka_producer_offset", String.valueOf(recordMetadata.offset()));
        MDC.put("mdc_kafka_producer_timestamp", String.valueOf(recordMetadata.timestamp()));
        log.info("kafka message produce ok");
    }
}
