package tools.resilience4j;

import infrastructure.logger.MdcKeys;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
public class RetryWrapper {

    public static <T> T retry(Supplier<T> supplier,
                              int maxAttempts, long waitDuration) {
        // retry setup
        AtomicInteger currentAttempt = new AtomicInteger(1);
        String retryId = "retry-id" + UUID.randomUUID();
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(waitDuration))
                .retryExceptions(Throwable.class)
                .build();
        Retry retry = Retry.of(retryId, retryConfig);
        retry.getEventPublisher()
                .onRetry(event -> {
                            MDC.put(MdcKeys.mdc_retry_current_attempt.name(), String.valueOf(currentAttempt.get()));
                            log.warn("error detected, going to retry. current attempt: {}", currentAttempt);
                            currentAttempt.getAndIncrement();
                        }
                );

        // execute wrapped supplier
        return Retry.decorateSupplier(retry, supplier).get();
    }


}
