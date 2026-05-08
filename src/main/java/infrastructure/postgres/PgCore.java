package infrastructure.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.File;

public class PgCore {

    private static final Logger log = LoggerFactory.getLogger(PgCore.class);

    public static final String ORG_POSTGRESQL_DRIVER = "org.postgresql.Driver";

    public static NamedParameterJdbcTemplate getNamedParameterJdbcTemplate(String url, String id, String pass) {
        HikariDataSource hikariDataSource = getHikariDataSource(url, id, pass);
        return new NamedParameterJdbcTemplate(hikariDataSource);
    }

    public static HikariDataSource getHikariDataSource(String url, String id, String pass) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(id);
        hikariConfig.setPassword(pass);
        hikariConfig.setDriverClassName(ORG_POSTGRESQL_DRIVER);
        return new HikariDataSource(hikariConfig);
    }

    public static void copyFromCsv(JdbcTemplate jdbcTemplate, String schema, String table, String path, String file) {
        long start = System.currentTimeMillis();
        String absolutePath = new File(path, file).getAbsolutePath();
        log.info("copy csv. file : {} , schema : {} , table : {}", absolutePath, schema, table);

        String sql = """
                COPY %s.%s
                FROM '%s'
                WITH (FORMAT CSV, HEADER, DELIMITER ',');
                """.formatted(schema, table, absolutePath, file);

        jdbcTemplate.execute(sql);
        long time = System.currentTimeMillis() - start;
    }


}
