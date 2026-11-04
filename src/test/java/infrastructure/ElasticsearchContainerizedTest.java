package infrastructure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import com.holdenkarau.spark.testing.JavaDatasetSuiteBase;
import infrastructure.spark.SparkCore;
import infrastructure.spark.SparkElastic;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import tools.java.TrustStore;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.Optional;

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
public class ElasticsearchContainerizedTest {

    // elastic container
    static ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.5"));
    static String url;
    static String id;
    static String pass;
    static SSLContext sslContext;
    static TrustStore trustStore;

    // spark
    SparkSession spark = SparkCore.getUnitTestSparkSession();
    JavaDatasetSuiteBase javaDatasetSuiteBase = new JavaDatasetSuiteBase();

    @BeforeAll
    static void beforeAll() throws Exception {
        elasticsearchContainer.start();
        url = elasticsearchContainer.getHttpHostAddress();
        id = "elastic";
        pass = ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD;
        sslContext = elasticsearchContainer.createSslContextFromCa();
        // for spark
        Optional<byte[]> bytes = elasticsearchContainer.caCertAsBytes();
        trustStore = TrustStore.fromCaCertificate(bytes.get());
    }

    @AfterAll
    static void afterAll() {
        elasticsearchContainer.stop();
    }

    @Test
    void test_spark_read_to_from_elastic_should_success() throws IOException {
        // given
        Dataset<Row> df = spark.read().option("header", true).csv("src/test/resources/samples/s1.csv");
        ElasticsearchClient elasticsearchClient = ElasticsearchClient.of(
                builder ->
                        builder.host("https://" + url)
                                .usernameAndPassword(id, pass)
                                .sslContext(sslContext)
        );
        CreateIndexResponse createIndexResponse = elasticsearchClient.indices().create(c -> c.index("unit-test-container-index"));

        // when
        SparkElastic.write(df, url, id, pass, trustStore, "unit-test-container-index");
        Dataset<Row> dfIdx = spark.read()
                .format("org.elasticsearch.spark.sql")
                .option("es.resource", "unit-test-container-index")
                // http
                .option("es.nodes.wan.only", "true")
                .option("es.nodes", url)
                .option("es.net.http.auth.user", id)
                .option("es.net.http.auth.pass", pass)
                // ssl
                .option("es.net.ssl", "true")
                .option("es.net.ssl.truststore.location", trustStore.path().toAbsolutePath().toUri().toString())
                .option("es.net.ssl.truststore.pass", trustStore.password())
                .option("es.net.ssl.truststore.type", "PKCS12")
                .load();

        // then
        javaDatasetSuiteBase.assertDatasetEquals(
                df.select("id","pays","ville","population"),
                dfIdx.select("id","pays","ville","population"));
    }

}
