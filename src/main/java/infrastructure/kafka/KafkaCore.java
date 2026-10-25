package infrastructure.kafka;

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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static infrastructure.logger.MdcKeys.*;

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
     * blocking send
     */
    static public <K, V> void produce(KafkaProducer<K, V> kafkaProducer,
                                      ProducerRecord<K, V> producerRecord) throws ExecutionException, InterruptedException {

        long startTime = System.currentTimeMillis();
        Map<String, String> logMdc = new TreeMap<>();

        RecordMetadata recordMetadata = kafkaProducer.send(producerRecord).get();

        // log
        logMdc.put(mdc_kafka_producer_topic.name(), recordMetadata.topic());
        logMdc.put(mdc_kafka_producer_partition.name(), String.valueOf(recordMetadata.partition()));
        logMdc.put(mdc_kafka_producer_offset.name(), String.valueOf(recordMetadata.offset()));
        logMdc.put(mdc_kafka_producer_timestamp.name(), String.valueOf(recordMetadata.timestamp()));
        long endTime = System.currentTimeMillis() - startTime;
        logMdc.put(mdc_kafka_producer_send_time_ms.name(), String.valueOf(endTime));
        logMdc.forEach(MDC::put);
        log.info("kafka message produce ok. time ms : {}", endTime);
    }
}
