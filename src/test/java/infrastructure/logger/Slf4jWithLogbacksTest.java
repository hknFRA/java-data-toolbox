package infrastructure.logger;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class Slf4jWithLogbackTest {


    private static final Logger log = LoggerFactory.getLogger(Slf4jWithLogbackTest.class);

    @Test
    void test_change_programmatically_log_level() {
        log.debug("test");
        Slf4jWithLogback.changeLevel(Map.of("ROOT", "DEBUG"));
        log.debug("test");
        Slf4jWithLogback.changeLevel(Map.of("ROOT", "TRACE"));
        log.debug("test");
        log.trace("test");
    }

}