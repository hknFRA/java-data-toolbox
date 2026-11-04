package infrastructure.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import tools.java.TrustStore;

import static org.elasticsearch.hadoop.cfg.ConfigurationOptions.*;

// elastic + testcontainers
// https://testcontainers.com/modules/elasticsearch/
// https://www.elastic.co/search-labs/blog/elasticsearch-integration-tests
// https://stackoverflow.com/a/79860621/6215824
//
// elastic + spark
// https://www.elastic.co/docs/reference/elasticsearch-hadoop/installation
// https://www.elastic.co/docs/reference/elasticsearch-hadoop/apache-spark-support#spark-sql
// https://www.elastic.co/docs/reference/elasticsearch-hadoop/configuration#_advanced_settings
// https://github.com/elastic/elasticsearch-hadoop/tree/main#installation
//
// elastic + java native
// https://github.com/elastic/elasticsearch-java/tree/main/examples/realworld-app
// https://github.com/elastic/elasticsearch-java/blob/main/examples/realworld-app/rw-database/src/test/java/realworld/db/ElasticClientTest.java
public class SparkElastic {

    public static Dataset<Row> read(SparkSession spark, String url, String id, String pass, TrustStore trustStore, String index) {
        return spark.read()
                .format("org.elasticsearch.spark.sql")
                .option(ES_RESOURCE, index)
                // http
                .option(ES_NODES, url)
                .option(ES_NODES_WAN_ONLY, "true")
                .option(ES_NET_HTTP_AUTH_USER, id)
                .option(ES_NET_HTTP_AUTH_PASS, pass)
                // ssl
                .option(ES_NET_USE_SSL, "true")
                .option(ES_NET_SSL_TRUST_STORE_LOCATION, trustStore.path().toAbsolutePath().toUri().toString())
                .option(ES_NET_SSL_TRUST_STORE_PASS, trustStore.password())
                .option("es.net.ssl.truststore.type", "PKCS12")
                .load();
    }

    public static void write(Dataset<Row> df, String url, String id, String pass, TrustStore trustStore, String index) {
        df.write()
                .format("org.elasticsearch.spark.sql")
                .option(ES_RESOURCE, index)
                // http
                .option(ES_NODES, url)
                .option(ES_NODES_WAN_ONLY, "true")
                .option(ES_NET_HTTP_AUTH_USER, id)
                .option(ES_NET_HTTP_AUTH_PASS, pass)
                // ssl
                .option(ES_NET_USE_SSL, "true")
                .option(ES_NET_SSL_TRUST_STORE_LOCATION, trustStore.path().toAbsolutePath().toUri().toString())
                .option(ES_NET_SSL_TRUST_STORE_PASS, trustStore.password())
                .option("es.net.ssl.truststore.type", "PKCS12")
                .save();
    }
}
