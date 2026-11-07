package infrastructure.logger;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

// interface (facade) : slf4j
// implementation (engine) : logback
public class Slf4jWithLogback {

    private static final Logger LOG = LoggerFactory.getLogger(Slf4jWithLogback.class);

    public static void enableTechnicalLog() {

    }

    public static void enableLog(String logback) {

    }

    public static void changeLevel(Map<String, String> loggerLevelMap) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerLevelMap.forEach((aLoggerName, aLoggerLevel) ->
                Optional.ofNullable(loggerContext.exists(aLoggerName))
                        .ifPresent(aLogger -> {
                            ch.qos.logback.classic.Level safeLevel = ch.qos.logback.classic.Level.valueOf(aLoggerLevel);
                            aLogger.setLevel(safeLevel);
                        }));
    }


}
