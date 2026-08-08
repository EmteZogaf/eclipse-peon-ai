package org.sterl.llmpeon.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CancellationException;

import org.junit.jupiter.api.Test;

import dev.langchain4j.exception.RateLimitException;

class ExceptionUtilTest {

    @Test
    void isCanceled_returns_true_for_direct_cancellation() {
        assertThat(ExceptionUtil.isCanceled(new CancellationException())).isTrue();
    }

    @Test
    void isCanceled_returns_true_for_nested_cancellation() {
        var cause = new CancellationException("aborted");
        assertThat(ExceptionUtil.isCanceled(new RuntimeException("wrap", cause))).isTrue();
    }

    @Test
    void isCanceled_returns_true_for_deeply_nested_cancellation() {
        var root = new CancellationException("aborted");
        var mid  = new IllegalStateException("mid", root);
        var top  = new RuntimeException("top", mid);
        assertThat(ExceptionUtil.isCanceled(top)).isTrue();
    }

    @Test
    void isCanceled_returns_false_for_plain_exception() {
        assertThat(ExceptionUtil.isCanceled(new RuntimeException("boom"))).isFalse();
    }

    @Test
    void isCanceled_returns_false_for_null() {
        assertThat(ExceptionUtil.isCanceled(null)).isFalse();
    }

    @Test
    void isRateLimit_returns_true_for_direct_rate_limit() {
        assertThat(ExceptionUtil.isRateLimit(new RateLimitException("limit"))).isTrue();
    }

    @Test
    void isRateLimit_returns_true_for_nested_rate_limit() {
        var cause = new RateLimitException("limit");
        assertThat(ExceptionUtil.isRateLimit(new RuntimeException("wrap", cause))).isTrue();
    }

    @Test
    void isRateLimit_returns_false_for_plain_exception() {
        assertThat(ExceptionUtil.isRateLimit(new RuntimeException("boom"))).isFalse();
    }

    @Test
    void isRateLimit_returns_false_for_null() {
        assertThat(ExceptionUtil.isRateLimit(null)).isFalse();
    }
}
