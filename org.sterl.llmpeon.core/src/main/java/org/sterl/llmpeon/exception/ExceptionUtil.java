package org.sterl.llmpeon.exception;

import java.util.concurrent.CancellationException;

import dev.langchain4j.exception.RateLimitException;

/**
 * Shared exception helpers used across core and plugin layers.
 */
public final class ExceptionUtil {

    private ExceptionUtil() {}

    /**
     * Returns {@code true} when the given throwable or any of its nested causes
     * is a {@link CancellationException}.
     */
    public static boolean isCanceled(Throwable throwable) {
        if (throwable == null) return false;
        Throwable current = throwable;
        do {
            if (current instanceof CancellationException) return true;
            current = current.getCause();
        } while (current != null && current != throwable);
        return false;
    }

    /**
     * Returns {@code true} when the given throwable or any of its nested causes
     * is a {@link RateLimitException}.
     */
    public static boolean isRateLimit(Throwable throwable) {
        if (throwable == null) return false;
        Throwable current = throwable;
        do {
            if (current instanceof RateLimitException) return true;
            current = current.getCause();
        } while (current != null && current != throwable);
        return false;
    }
}
