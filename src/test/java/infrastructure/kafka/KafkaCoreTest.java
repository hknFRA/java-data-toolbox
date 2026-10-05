package infrastructure.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

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
            //kafkaConsumer = new KafkaConsumer<>();

            // producer
            //kafkaProducer = new KafkaProducer<>();
        }
    }

    @Test
    void given_consumer_client_should_consume_message() {
        KafkaCore.consume(kafkaConsumer, 1000L, null);
    }


}