package infrastructure.cassandra;

import infrastructure.flyway.FlywayCore;
import org.junit.jupiter.api.Test;
import org.testcontainers.cassandra.CassandraContainer;

import java.net.InetSocketAddress;

public class CassandraContainerizedTest {

    static CassandraContainer cassandraContainer = new CassandraContainer("cassandra:6.0");

    @Test
    void test_cassandra_containerized() {
        cassandraContainer.start();
        InetSocketAddress contactPoint = cassandraContainer.getContactPoint();
        String username = cassandraContainer.getUsername();
        String password = cassandraContainer.getPassword();
        String localDatacenter = cassandraContainer.getLocalDatacenter();


        // given
        String host = contactPoint.toString().replaceAll("localhost/", "");

        FlywayCore.flywayInitTablesCassandraNative("jdbc:cassandra://" + host + "?localdatacenter=" + localDatacenter, username, password,
                "db-testing/cassandra/migration", "tab_audit_flyway_history");

        // when
        System.out.println();
        // then

    }

}
