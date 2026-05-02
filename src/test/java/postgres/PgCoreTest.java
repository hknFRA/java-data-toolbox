package postgres;

import infrastructure.postgres.PgCore;
import org.junit.jupiter.api.Test;

class PgCoreTest {


    @Test
    void test_given_jdbc_client_should_establish_connection_with_postgres() {
        PgCore.get();
    }

}