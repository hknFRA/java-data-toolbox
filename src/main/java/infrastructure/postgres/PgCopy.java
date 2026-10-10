package infrastructure.postgres;

import com.zaxxer.hikari.HikariDataSource;
import infrastructure.jdbc.JdbcSecrets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

@Slf4j
public class PgCopy {


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
     * use native pg CopyManager for efficiency <p>
     * CopyManager enable query to stdout <p>
     * https://stackoverflow.com/questions/27154579/how-to-export-data-from-postgresql-to-csv-file-using-jdbc
     */
    public static void copyTableToCsv(JdbcTemplate jdbcTemplate,
                                      String schema,
                                      String table,
                                      String path,
                                      String file) {

        long start = System.currentTimeMillis();
        String absolutePath = new File(path, file).getAbsolutePath();
        Map<String, Object> logParams = new java.util.HashMap<>(Map.of("schema", schema, "table", table, "absolute path", absolutePath));
        log.info("start copy csv. {}", logParams);

        JdbcSecrets jdbcSecrets = JdbcSecrets.getJdbcSecrets((HikariDataSource) jdbcTemplate.getDataSource());
        String sql = """
                COPY %s.%s
                TO STDOUT
                WITH (FORMAT CSV,
                HEADER TRUE,
                DELIMITER ',');
                """.formatted(schema, table);

        try {
            // jdbcTemplate.execute(sql) throws error

            File targetFile = new File(path + file);
            boolean delete = targetFile.exists() ? targetFile.delete() : true;

            // pg CopyManager enable query to stdout
            FileUtils.touch(targetFile);
            FileOutputStream fileOutputStream = new FileOutputStream(targetFile);
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
}
