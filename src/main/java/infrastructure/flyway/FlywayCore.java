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

    public static void flywayInitTablesCassandra(String url, String id, String password,
                                                 String scriptsLocation, String historyTable) {

        // Enable native connectors for Cassandra


        boolean isCassandra = url.startsWith("cassandra://");
        boolean nativeConnectorsTurnedOn = NativeConnectorsModeUtils.isNativeConnectorsTurnedOn();

        if (isCassandra) {
            log.info("Cassandra detected. Native Connectors enabled: {}", nativeConnectorsTurnedOn);
            if (!nativeConnectorsTurnedOn) {
                String msg = String.join("\n\t",
                        "FLYWAY_NATIVE_CONNECTORS=true environment variable must be set",
                        "IntelliJ : edit run configuration with env var FLYWAY_NATIVE_CONNECTORS=true",
                        "maven : configured via Maven Surefire plugin in pom.xml");
                throw new RuntimeException(msg);
            }
            // Set Cassandra-specific Flyway properties
            System.setProperty("flyway.url", url);
            System.setProperty("flyway.user", id);
            System.setProperty("flyway.password", password);
            System.setProperty("flyway.sqlMigrationSuffixes", ".cql");
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

        Flyway flyway = flywayConf.load();
        flyway.migrate();
    }


}
