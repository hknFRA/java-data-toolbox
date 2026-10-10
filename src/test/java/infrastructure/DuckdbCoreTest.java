package infrastructure;

import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;

class DuckdbCoreTest {

    @Test
    void test_init_jdbc_template_on_duckdb() {
        // given
        String absolutePath = "/Users/hakan/IdeaProjects/java-data-toolbox/src/test/resources/samples/s1.csv";

        // when
        NamedParameterJdbcTemplate duckDbClient = DuckdbCore.getDuckDbClient("jdbc:duckdb:memory", "", "");
        String s = DuckdbCore.loadCsv(duckDbClient, absolutePath);

        // result
        List<Map<String, @Nullable Object>> maps = duckDbClient.getJdbcTemplate().queryForList("select * from %s".formatted(s));
        List<Map<String, @Nullable Object>> max = duckDbClient.getJdbcTemplate().queryForList("select max(population) as _max from %s".formatted(s));

        // then
        Assertions.assertThat(maps).hasSize(3);
        Assertions.assertThat(max.getFirst().get("_max")).isEqualTo(200000L);
        System.out.println();
    }

}