package infrastructure.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import infrastructure.JdbcCore;
import org.postgresql.PGProperty;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class PgCore {

    private static final Logger log = LoggerFactory.getLogger(PgCore.class);

    // safe driver name
    public static final String ORG_POSTGRESQL_DRIVER = org.postgresql.Driver.class.getCanonicalName();

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

    public static void copyCsvToTable(JdbcTemplate jdbcTemplate, String schema, String table, String path, String file) {
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

    /**
     *
     */
    public static void copyTableToCsv(JdbcTemplate jdbcTemplate, String schema, String table, String path, String file) {
        // https://stackoverflow.com/questions/27154579/how-to-export-data-from-postgresql-to-csv-file-using-jdbc

        long start = System.currentTimeMillis();
        String absolutePath = new File(path, file).getAbsolutePath();
        Map<String, Object> logParams = new java.util.HashMap<>(Map.of("schema", schema, "table", table, "absolute path", absolutePath));
        log.info("start copy csv. {}", logParams);

        JdbcCore.JdbcSecrets jdbcSecrets = JdbcCore.JdbcSecrets.getJdbcSecrets((HikariDataSource) jdbcTemplate.getDataSource());
        String sql = """
                COPY %s.%s
                TO STDOUT
                WITH (FORMAT CSV,
                HEADER TRUE,
                DELIMITER ',');
                """.formatted(schema, table);

        try {
            // jdbcTemplate.execute(sql) throws error

            boolean delete = new File(path + file).delete();
            FileOutputStream fileOutputStream = new FileOutputStream(path + file);

            // pg CopyManager enable query to stdout
            Connection connection = DriverManager.getConnection(jdbcSecrets.url(), jdbcSecrets.id(), jdbcSecrets.pass());
            CopyManager copyManager = new CopyManager((BaseConnection) connection);
            copyManager.copyOut(sql, fileOutputStream);

            long time = System.currentTimeMillis() - start;
            logParams.put("time ms", time);
            log.info("copy table to csv OK. {}", logParams);

        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void isPgPropertiesValid(Properties properties) {
        List<String> values = Arrays
                .stream(PGProperty.values())
                .map(PGProperty::getName)
                .toList();

        List<String> list = properties.values().stream().map(Object::toString).toList();
        if (!new HashSet<>(values).containsAll(list)) {
            throw new RuntimeException("unknown pg property detected");
        }
    }


}
