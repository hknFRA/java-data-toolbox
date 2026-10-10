package infrastructure.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import infrastructure.postgres.PgCore;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

public record JdbcSecrets(
        String url,
        String id,
        String pass
) {
    public static JdbcSecrets getJdbcSecrets(DataSource dataSource) {
        try {
            String url = dataSource.getConnection().getMetaData().getURL();
            String id = dataSource.getConnection().getMetaData().getUserName();

            String pass;
            if (dataSource.getConnection().getMetaData().getDriverName().equals(PgCore.ORG_POSTGRESQL_DRIVER)) {
                pass = ((PGSimpleDataSource) dataSource).getPassword();
            } else {
                pass = null;
            }

            return new JdbcSecrets(url, id, pass);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static JdbcSecrets getJdbcSecrets(HikariDataSource dataSource) {
        try {
            String url = dataSource.getConnection().getMetaData().getURL();
            String id = dataSource.getConnection().getMetaData().getUserName();
            String pass = null;
            return new JdbcSecrets(url, id, pass);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
