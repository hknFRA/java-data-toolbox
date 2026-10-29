package tools.resilience4j;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

// https://resilience4j.readme.io/docs/timeout
@Slf4j
public class TimeLimiterWrapper {

    public static TimeLimiter getTimeLimiter(long timeoutDuration) {
        String timeoutId = "timeout-id" + UUID.randomUUID();
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(timeoutDuration))
                .cancelRunningFuture(true)
                .build();
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutId, timeLimiterConfig);
        timeLimiter.getEventPublisher()
                .onError(event -> {
                    log.error("error detected");
                })
                .onTimeout(event -> {
                    log.warn("timeout reached");
                })
                .onSuccess(event -> {
                    log.info("OK");
                });
        return timeLimiter;
    }

    // todo : not working
    public static <T> T timeout(Supplier<T> supplier, long timeoutDuration) throws Exception {
        Future<Supplier<T>> future = CompletableFuture.completedFuture(supplier);
        Supplier<Future<Supplier<T>>> supplierFuture = () -> future;
        TimeLimiter timeLimiter = getTimeLimiter(timeoutDuration);
        return TimeLimiter.decorateFutureSupplier(timeLimiter, supplierFuture).call().get();
    }

}
