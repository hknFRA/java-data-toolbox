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

        ElasticsearchClient elasticsearchClient = ElasticsearchClient.of(
                builder ->
                        builder.host("https://" + url)
                                .usernameAndPassword(id, pass)
                                .sslContext(sslContext)
        );
       elasticsearchClient.indices().create(c -> c.index("unit-test-container-index"));
       elasticsearchClient.indices().create(c -> c.index("unit-test-container-index-2"));

    }

    @AfterAll
    static void afterAll() {
        elasticsearchContainer.stop();
    }

    @Test
    void test_spark_read_to_from_elastic_should_success() throws IOException {
        // given
        Dataset<Row> df = spark.read().option("header", true).csv("src/test/resources/samples/s1.csv");
        String index = "unit-test-container-index";

        // when
        SparkElastic.write(df, url, id, pass, trustStore, index);
        Dataset<Row> dfIdx = SparkElastic.read(spark, url, id, pass, trustStore, index);

        // then
        javaDatasetSuiteBase.assertDatasetEquals(
                df.select("id","pays","ville","population"),
                dfIdx.select("id","pays","ville","population"));
    }

    @Test
    void test_spark_json_write_to_elastic_should_success() throws IOException {
        // given
        Dataset<Row> df = spark.read().option("multiLine", true).json("src/test/resources/samples/s4.ndjson");
        String index = "unit-test-container-index-2";

        // when
        SparkElastic.write(df, url, id, pass, trustStore, index);
        Dataset<Row> dfIdx = SparkElastic.read(spark, url, id, pass, trustStore, index);

        // then
        javaDatasetSuiteBase.assertDatasetEquals(
                df.select("id","pays","ville","population"),
                dfIdx.select("id","pays","ville","population"));
    }

}
