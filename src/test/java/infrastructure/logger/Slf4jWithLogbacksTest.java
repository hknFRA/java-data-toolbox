package infrastructure.logger;

import ch.qos.logback.core.joran.spi.JoranException;
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

    @Test
    void test_add_logback() throws JoranException {
        log.debug("test");
        Slf4jWithLogback.addLogbackConfiguration("/Users/hakan/IdeaProjects/java-data-toolbox/src/main/resources/logback.xml");
        //Slf4jWithLogback.changeLevel(Map.of("ROOT", "DEBUG"));
        log.trace("test");
        log.info("test");
        //Slf4jWithLogback.changeLevel(Map.of("ROOT", "TRACE"));

    }

}