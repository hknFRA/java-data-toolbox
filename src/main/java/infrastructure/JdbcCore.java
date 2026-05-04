package infrastructure;

import javax.sql.DataSource;
import java.sql.SQLException;

public class JdbcCore {

    public record JdbcSecrets(
            String url,
            String id,
            String pass
    ) {
        public static JdbcSecrets getJdbcSecrets(DataSource dataSource) {
            try {
                String url = dataSource.getConnection().getMetaData().getURL();
                String id = dataSource.getConnection().getMetaData().getUserName();
                return new JdbcSecrets(url, id, "");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
