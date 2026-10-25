package infrastructure.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

// https://stackoverflow.com/questions/76946151/embeddedkafka-without-spring-boot-in-juint5
@EmbeddedKafka(topics = KafkaCoreTest.TOPIC)
class KafkaCoreTest {

    // params
    static final String TOPIC = "my-topic";

    // init vars
    boolean initOk;
    static KafkaConsumer<String, String> kafkaConsumer;
    static KafkaProducer<String, String> kafkaProducer;

    @BeforeEach
    void beforeEach(EmbeddedKafkaBroker broker) {
        // inject kafka broker from before all

        if (!initOk) {
            // producer
            Map<String, Object> producerProps = KafkaTestUtils.producerProps(broker);
            kafkaProducer = KafkaClients.getSimpleProducer(producerProps.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG).toString());

            // consumer
            Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(broker, UUID.randomUUID().toString(), false);
            kafkaConsumer = KafkaClients.getSimpleConsumer(consumerProps.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG).toString(), List.of(TOPIC));

            initOk = true;
        }
    }

    @AfterAll
    static void afterAll() {
        kafkaConsumer.close();
        kafkaProducer.close();
    }

    /**
     * seems that poll timeout is reached differently depending on chipset
     * M2 Pro : 100L OK
     * M5 : 1000L OK
     */
    @Test
    void given_consumer_client_should_consume_message() throws ExecutionException, InterruptedException {
        // given
        ProducerRecord<String, String> bonjour = new ProducerRecord<>(TOPIC, "1", "bonjour");
        RecordMetadata r = kafkaProducer.send(bonjour).get();
        RecordMetadata r2 = kafkaProducer.send(bonjour).get();

        // when
        ConsumerRecords<String, String> poll = kafkaConsumer.poll(Duration.ofMillis(1000L));

        // then
        Assertions.assertThat(poll.count()).isGreaterThan(0);
    }

    @Test
    void given_wrapped_consumer_producer_methods_should_consume_then_produce_message() throws ExecutionException, InterruptedException {
        // given
        ProducerRecord<String, String> bonjour = new ProducerRecord<>(TOPIC, "1", "bonjour");
        Function<ConsumerRecord<String, String>, Boolean> function =
                input -> {
                    System.out.println(input);
                    return true;
                };

        // when
        KafkaCore.produce(kafkaProducer, bonjour);
        KafkaCore.consume(kafkaConsumer, 10000L, function);
    }


}