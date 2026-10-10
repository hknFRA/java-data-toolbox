package infrastructure;

import infrastructure.jdbc.JdbcTemplateClients;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.UUID;

public class DuckdbCore {

    public static final String ORG_DUCKDB_DUCK_DBDRIVER = "org.duckdb.DuckDBDriver";

    public static NamedParameterJdbcTemplate getDuckDbClient(String url,
                                                             String id,
                                                             String pass) {
        return JdbcTemplateClients.getNamedParameterJdbcTemplate(url, id, pass, ORG_DUCKDB_DUCK_DBDRIVER);
    }

    /**
     *
     */
    public static String loadCsv(NamedParameterJdbcTemplate duckdbClient, String absolutePath) {
        String uuid = "csv_" + UUID.randomUUID().toString().replace("-", "_");
        String sql = """
                CREATE TABLE %s AS
                SELECT * FROM '%s';
                """.formatted(uuid, absolutePath);
        duckdbClient.getJdbcTemplate().execute(sql);
        return uuid;
    }

}
