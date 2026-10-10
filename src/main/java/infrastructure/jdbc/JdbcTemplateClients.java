package infrastructure.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * generic wrapper of jdbc template
 * compatible with : postgres, duckdb
 */
public class JdbcTemplateClients {

    public static NamedParameterJdbcTemplate getNamedParameterJdbcTemplate(String url,
                                                                           String id,
                                                                           String pass,
                                                                           String driver) {

        HikariDataSource hikariDataSource = getHikariDataSource(url, id, pass, driver);
        return new NamedParameterJdbcTemplate(hikariDataSource);
    }

    public static HikariDataSource getHikariDataSource(String url,
                                                       String id,
                                                       String pass,
                                                       String driver) {

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(id);
        hikariConfig.setPassword(pass);
        hikariConfig.setDriverClassName(driver);
        return new HikariDataSource(hikariConfig);
    }

}
