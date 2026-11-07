package tools.logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/// interface (facade) : slf4j
/// {{@link org.slf4j.Logger}}
///
/// implementation (engine) : logback
/// {{@link ch.qos.logback.classic.Logger}}
/// {{@link ch.qos.logback.classic.Level}}
///
/// https://javaetmoi.com/2018/03/configurez-logback-en-java/
public class Slf4jWithLogback {
// todo : study listener vs. appender

    private static final Logger LOG = LoggerFactory.getLogger(Slf4jWithLogback.class);

    public static void changeLevel(Map<String, String> loggerLevelMap) {
        Map<String, Level> loggerLevelMapSafe = new TreeMap<>();
        loggerLevelMap.forEach((aLoggerName, aLoggerLevel) ->
                loggerLevelMapSafe.put(aLoggerName, Level.valueOf(aLoggerLevel)));
        _changeLevel(loggerLevelMapSafe);
    }

    /// internal method with safe {@link Level} signature
    ///
    /// note : if logback level is null, class logger will take leve of is root logger
    private static void _changeLevel(Map<String, Level> loggerLevelMap) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerLevelMap.forEach((aLoggerName, aLoggerLevel) ->
                Optional.ofNullable(loggerContext.exists(aLoggerName))
                        .ifPresent(aLogger -> aLogger.setLevel(aLoggerLevel)));
    }


    ///
    public static void addLogbackConfiguration(String logbackPath) throws JoranException {
        // context
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Map<String, Level> tmpLogLevel = new TreeMap<>();
        loggerContext.getLoggerList().forEach(logger ->
                tmpLogLevel.put(logger.getName(), logger.getLevel()));

        // configure added XML
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        configurator.doConfigure(logbackPath);

        // restore initial logger level
        _changeLevel(tmpLogLevel);
    }


}
