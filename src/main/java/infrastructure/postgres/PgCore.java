package infrastructure.postgres;

import org.postgresql.PGProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PgCore {

    private static final Logger log = LoggerFactory.getLogger(PgCore.class);

    // safe driver name
    public static final String ORG_POSTGRESQL_DRIVER = org.postgresql.Driver.class.getCanonicalName();

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
