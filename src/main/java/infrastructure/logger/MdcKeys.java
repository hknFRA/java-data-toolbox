package infrastructure.logger;

/**
 * keys to be used in logback-json.xml
 */
public enum MdcKeys {

    // kafka consumer
    mdc_kafka_consumer_group,
    mdc_kafka_consumer_group_id,
    mdc_kafka_consumer_member_id,
    mdc_kafka_consumer_topic,
    mdc_kafka_consumer_partition,
    mdc_kafka_consumer_offset,

    // kafka producer
    mdc_kafka_producer_topic,
    mdc_kafka_producer_partition,
    mdc_kafka_producer_offset,
    mdc_kafka_producer_timestamp,

    // spark JDBC
    mdc_spark_jdbc_url,
    mdc_spark_jdbc_user,
    mdc_spark_jdbc_time_ms,
}
