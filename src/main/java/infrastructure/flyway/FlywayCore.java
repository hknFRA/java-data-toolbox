package infrastructure.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.internal.nc.NativeConnectorsModeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlywayCore {

    private static final Logger log = LoggerFactory.getLogger(FlywayCore.class);

    public static void flywayInitTables(String jdbcUrl, String id, String password,
                                        String scriptsLocation, String historyTable) {
        // https://medium.com/@AlexanderObregon/automating-database-migrations-in-java-with-flyway-4139cb4a748d
        // warn : by default flyway check automatically next folder to retrieve sql -> src/main/resources/db/migration
        // on cloud, files seems to be located in /opt/spark/work-dir/file:/open-rp-batch.jar!/db/migration/

        log.info("Flyway init tables ...");

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, id, password)
                .locations(scriptsLocation)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .failOnMissingLocations(true)
                .installedBy(id)
                .table(historyTable)
                .connectRetries(3)
                .connectRetriesInterval(1)
                .load();
        flyway.migrate();
    }


    public static void flywayInitTablesCassandraWrapper(String url, String id, String password,
                                                        String scriptsLocation, String historyTable) {

        Flyway flyway = Flyway.configure()
                .locations(scriptsLocation)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .failOnMissingLocations(true)
                .installedBy(id)
                .table(historyTable)
                .connectRetries(3)
                .connectRetriesInterval(1)
                .dataSource(url, id, password)
                .sqlMigrationSuffixes(".cql")
                .load();
        flyway.migrate();
    }


    /**
     * TODO
     * https://github.com/flyway/flyway/issues/4190
     * https://documentation.red-gate.com/fd/cassandra-database-native-connectors-341246466.html
     * https://documentation.red-gate.com/flyway/reference/tutorials/tutorial-using-native-connectors-to-connect-to-mongodb
     * https://documentation.red-gate.com/fd/flyway-native-connectors-mongodb-271583122.html
     */
    public static void flywayInitTablesCassandraNative(String url, String id, String password,
                                                       String scriptsLocation, String historyTable) {

        boolean nativeConnectorsTurnedOn = NativeConnectorsModeUtils.isNativeConnectorsTurnedOn();
        if (!nativeConnectorsTurnedOn) {
            String msg = String.join("\n\t",
                    "FLYWAY_NATIVE_CONNECTORS=true environment variable must be set",
                    "IntelliJ : edit run configuration with env var FLYWAY_NATIVE_CONNECTORS=true",
                    "maven : configured via Maven Surefire plugin in pom.xml");
            throw new RuntimeException(msg);
        }

        // Force loading of Cassandra plugin
        try {
            Class.forName("org.flywaydb.database.cassandra.CassandraDatabaseType");
            log.info("Cassandra plugin loaded successfully");
        } catch (ClassNotFoundException e) {
            log.error("Failed to load Cassandra plugin", e);
            throw new RuntimeException("Cassandra plugin not found", e);
        }

        FluentConfiguration flywayConf = Flyway.configure()
                .locations(scriptsLocation)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .failOnMissingLocations(true)
                .installedBy(id)
                .table(historyTable)
                .connectRetries(3)
                .connectRetriesInterval(1);

        // Configure Cassandra native connector via properties
        // java.util.Properties props = new java.util.Properties();
        // props.setProperty("flyway.url", url);
        // props.setProperty("flyway.user", id);
        // props.setProperty("flyway.password", password);
        // props.setProperty("flyway.sqlMigrationSuffixes", ".cql");

        flywayConf = flywayConf
                .driver("org.apache.cassandra")
                .communityDBSupportEnabled(false)
                .dataSource(url, id, password)
                .sqlMigrationSuffixes(".cql");

        Flyway flyway = flywayConf.load();
        flyway.migrate();
    }


}
