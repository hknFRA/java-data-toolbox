package tools.resilience4j;

import com.codahale.metrics.Gauge;
import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

class RetryWrapperTest {


    @Test
    void test_given_supplier_should_retry() {
        // given
        int maxAttempts = 3;
        long waitDuration = 100L;
        Supplier<Object> supplier = () -> {
            throw new RuntimeException();
        };

        // when
        long start = System.currentTimeMillis();
        try {
            Object none = RetryWrapper.retry(supplier, maxAttempts, waitDuration);
        } catch (RuntimeException e) {
        }
        long time = System.currentTimeMillis() - start;

        // then
        Assert.assertTrue(time > 200);
    }

    @Test
    void test_given_runnable_should_retry() {
        // given
        int maxAttempts = 3;
        long waitDuration = 100L;
        Runnable runnable = () -> {
            throw new RuntimeException();
        };

        // when
        long start = System.currentTimeMillis();
        try {
            RetryWrapper.retry(runnable, maxAttempts, waitDuration);
        } catch (RuntimeException e) {
        }
        long time = System.currentTimeMillis() - start;

        // then
        Assert.assertTrue(time > 200);
    }

    @Test
    @Disabled
    void test_given_supplier_should_timeout() throws Exception {
        // given
        long duration = 1L;
        Supplier<Object> supplier = () -> "bonjour";

        // when
        long start = System.currentTimeMillis();
        try {
            Object none = TimeLimiterWrapper.timeout(supplier, duration);
        } catch (Exception e) {
        }
        long time = System.currentTimeMillis() - start;

        // then
        Assert.assertTrue(time > 1000L);
    }


}