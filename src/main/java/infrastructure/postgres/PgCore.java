package infrastructure.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class PgCore {


    private static final Logger log = LoggerFactory.getLogger(PgCore.class);
    public static final String ORG_POSTGRESQL_DRIVER = "org.postgresql.Driver";

    public static void get() {
        log.info("test started");
    }

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


}
