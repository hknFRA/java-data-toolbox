package infrastructure.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.PGProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

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
