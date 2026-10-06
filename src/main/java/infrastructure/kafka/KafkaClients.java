package infrastructure.kafka;

import org.apache.kafka.clients.consumer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

public class KafkaClients {

    private static final Logger log = LoggerFactory.getLogger(KafkaClients.class);

    /**
     * manual commit
     */
    public static <K, V> KafkaConsumer<K, V> getSimpleConsumer(String servers,
                                                               String groupId,
                                                               String keyDeserializerClass,
                                                               String valueDeserializer,
                                                               List<String> topics) {
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializerClass);
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);

        KafkaConsumer<K, V> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(topics);
        return consumer;
    }
}
