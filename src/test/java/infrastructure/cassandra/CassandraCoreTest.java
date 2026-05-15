package infrastructure.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import infrastructure.flyway.FlywayCore;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

class CassandraCoreTest {

    @Test
    void test_flyway_cassandra_migration() {
        FlywayCore.flywayInitTablesCassandra(
                "cassandra://37583650-e472-4654-9711-44620fa410ee-us-east-2.db.astra.datastax.com:29042/unit_test?localdatacenter=us-east-2",
                "YXfHOsWIFuCWORcYKKLbfMpB",
                "D1YP7CKB.nkqJ5Ofx,5I3-9t1KmHZZ,jOs+ZSMORmMLGCHEos_TZyUe23KwJAvkmu.EQYm.BgfAwafAF2.n62t.I959p.6dfaAPkES-CQDpdcz__,zcDJQ,6-UwSlJCY",
                "/db-testing/cassandra/migration",
                "audit_cassandra_schema_history");
    }

    @Test
    void test_connect_to_cassandra() {

        // Create the CqlSession object:
        try (CqlSession session = CqlSession.builder()
                .withCloudSecureConnectBundle(Paths.get("local/secure-connect-cassandra-unit-test.zip"))
                .withAuthCredentials(
                        "YXfHOsWIFuCWORcYKKLbfMpB",
                        "D1YP7CKB.nkqJ5Ofx,5I3-9t1KmHZZ,jOs+ZSMORmMLGCHEos_TZyUe23KwJAvkmu.EQYm.BgfAwafAF2.n62t.I959p.6dfaAPkES-CQDpdcz__,zcDJQ,6-UwSlJCY")
                .build()) {

            // Select the release_version from the system.local table:
            ResultSet rs = session.execute("select release_version from system.local");
            Row row = rs.one();
            //Print the results of the CQL query to the console:
            if (row != null) {
                System.out.println(row.getString("release_version"));
            } else {
                System.out.println("An error occurred.");
            }
        }
    }

}