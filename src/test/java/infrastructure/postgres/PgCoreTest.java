package infrastructure.postgres;

import infrastructure.JdbcCore;
import io.zonky.test.db.postgres.embedded.FlywayPreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

class PgCoreTest {

    @RegisterExtension
    public static PreparedDbExtension db =
            EmbeddedPostgresExtension.preparedDatabase(
                    FlywayPreparer.forClasspathLocation("db-testing/postgres/migration"));

    JdbcCore.JdbcSecrets secrets = JdbcCore.JdbcSecrets.getJdbcSecrets(db.getTestDatabase());
    String url = secrets.url();
    String id = secrets.id();
    String pass = "";

    NamedParameterJdbcTemplate namedParameterJdbcTemplate = PgCore.getNamedParameterJdbcTemplate(url, id, pass);

    @BeforeEach
    void beforeEach() throws IOException {
        namedParameterJdbcTemplate.getJdbcTemplate().execute("truncate table public.table_countries");
        // FileUtils.delete(new File("src/test/out"));

    }

    @Test
    void test_given_csv_should_be_copy_to_table() {
        // given
        String schema = "public";
        String table = "table_countries";
        String path = "src/test/resources/samples/";
        String file = "s1.csv";

        // when
        PgCore.copyCsvToTable(namedParameterJdbcTemplate.getJdbcTemplate(), schema, table, path, file);

        // result
        Map<String, @Nullable Object> map = namedParameterJdbcTemplate.getJdbcTemplate().queryForMap("select count(*) as c_ , sum(population) as s_ from %s.%s".formatted(schema, table));

        // then
        Assertions.assertThat(map.get("c_")).isEqualTo(3L);
        Assertions.assertThat(map.get("s_")).isEqualTo(new BigDecimal(330000));
    }

    @Test
    void test_table_should_be_copy_to_csv() throws IOException {
        // given
        String schema = "public";
        String table = "table_countries";
        String pathIn = "src/test/resources/samples/";
        String fileIn = "s1.csv";
        String pathOut = "src/test/out/";
        String fileOut = "s1.csv";

        // when
        PgCore.copyCsvToTable(namedParameterJdbcTemplate.getJdbcTemplate(), schema, table, pathIn, fileIn);
        PgCore.copyTableToCsv(namedParameterJdbcTemplate.getJdbcTemplate(), schema, table, pathOut, fileOut);

        // result
        List<String> strings = FileUtils.readLines(new File(pathOut + fileOut), StandardCharsets.UTF_8);

        // then
        Assertions.assertThat(strings.size()).isEqualTo(4L); // header + data
    }

}