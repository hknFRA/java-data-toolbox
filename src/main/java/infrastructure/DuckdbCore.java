package infrastructure;

import infrastructure.jdbc.JdbcTemplateClients;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.UUID;

public class DuckdbCore {

    public record Result(
            String table,
            long timeMs,
            String path
    ) {
    }

    public static final String ORG_DUCKDB_DUCK_DB_DRIVER = org.duckdb.DuckDBDriver.class.getCanonicalName();

    public static NamedParameterJdbcTemplate getDuckDbClient(String url, String id, String pass) {
        return JdbcTemplateClients.getNamedParameterJdbcTemplate(url, id, pass, ORG_DUCKDB_DUCK_DB_DRIVER);
    }

    public static NamedParameterJdbcTemplate getDuckDbOnMemoryClient() {
        return DuckdbCore.getDuckDbClient("jdbc:duckdb:memory", "", "");
    }

    /**
     *
     */
    public static Result loadCsv(NamedParameterJdbcTemplate duckdbClient,
                                 String absolutePath) {
        long start = System.currentTimeMillis();
        String uuid = "csv_" + UUID.randomUUID().toString().replace("-", "_");
        String sql = """
                CREATE TABLE %s AS
                SELECT * FROM '%s';
                """.formatted(uuid, absolutePath);
        duckdbClient.getJdbcTemplate().execute(sql);

        long time = System.currentTimeMillis() - start;
        return new Result(uuid, time, absolutePath);
    }

    public static Result writeParquet(NamedParameterJdbcTemplate duckdbClient,
                                      String sourceTable,
                                      String targetPath,
                                      String targetFile) {
        long start = System.currentTimeMillis();

        String fullPath = targetPath + "/" + targetFile + ".parquet";
        String sql = """
                COPY %s
                TO '%s'
                (FORMAT parquet);
                """.formatted(sourceTable, fullPath);
        duckdbClient.getJdbcTemplate().execute(sql);

        long timeMs = System.currentTimeMillis() - start;
        return new Result(sourceTable, timeMs, fullPath);
    }

}
