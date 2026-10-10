package infrastructure;

import infrastructure.jdbc.JdbcTemplateClients;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.UUID;

public class DuckdbCore {

    public record Meta(String table) {
    }

    public static final String ORG_DUCKDB_DUCK_DB_DRIVER = org.duckdb.DuckDBDriver.class.getCanonicalName();

    public static NamedParameterJdbcTemplate getDuckDbClient(String url,
                                                             String id,
                                                             String pass) {
        return JdbcTemplateClients.getNamedParameterJdbcTemplate(url, id, pass, ORG_DUCKDB_DUCK_DB_DRIVER);
    }

    /**
     *
     */
    public static Meta loadCsv(NamedParameterJdbcTemplate duckdbClient, String absolutePath) {
        String uuid = "csv_" + UUID.randomUUID().toString().replace("-", "_");
        String sql = """
                CREATE TABLE %s AS
                SELECT * FROM '%s';
                """.formatted(uuid, absolutePath);
        duckdbClient.getJdbcTemplate().execute(sql);
        return new Meta(uuid);
    }

}
