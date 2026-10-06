package infrastructure.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.List;

// https://stackoverflow.com/questions/76946151/embeddedkafka-without-spring-boot-in-juint5
@EmbeddedKafka(topics = KafkaCoreTest.TOPIC)
class KafkaCoreTest {

    // params
    static final String TOPIC = "my-topic";

    // init vars
    boolean initOk;
    EmbeddedKafkaBroker broker;
    KafkaConsumer<String, String> kafkaConsumer;
    KafkaProducer<String, String> kafkaProducer;

    @BeforeEach
    void beforeEach(EmbeddedKafkaBroker broker) {
        // inject kafka broker from before all

        if (!initOk) {
            // consumer
            String servers = "localhost:0";
            kafkaConsumer = KafkaClients.getSimpleConsumer(servers, List.of(TOPIC));
            kafkaProducer = KafkaClients.getSimpleProducer(servers);
            initOk = true;
        }
    }

    @Test
    void given_consumer_client_should_consume_message() {
        ProducerRecord<String, String> bonjour = new ProducerRecord<>(TOPIC, "1", "bonjour");
        kafkaProducer.send(bonjour);
        KafkaCore.consume(kafkaConsumer, 1000L, null);
    }


}