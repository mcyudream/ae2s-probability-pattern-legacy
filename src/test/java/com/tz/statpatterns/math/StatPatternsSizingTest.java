package com.tz.statpatterns.math;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class StatPatternsSizingTest {
    @ParameterizedTest @CsvSource({"1,.5,.05", "4,.8,.05", "10,.5,.01", "30,.8,.01", "1,.00001,.05", "5,.999999,.01", "1,.2,.9"})
    void exactSizingMeetsTailAndIsMinimal(long target, double p, double alpha) {
        long attempts = StatPatternsSizing.planAttempts(target, p, alpha, 30).attempts();
        // Independent log-combination evaluation, not the production recurrence.
        assertTrue(tail(attempts, target, p) <= alpha + 1e-12);
        if (attempts > target) assertTrue(tail(attempts - 1, target, p) > alpha - 1e-12);
    }
    private static double tail(long n, long target, double p) {
        double sum = 0;
        for (int k = 0; k < target; k++) {
            double combination = 0;
            for (int i = 1; i <= k; i++) combination += Math.log(n - i + 1) - Math.log(i);
            sum += Math.exp(combination + k * Math.log(p) + (n - k) * Math.log1p(-p));
        }
        return sum;
    }
    @Test void deterministicAtLongLimit() {
        assertEquals(Long.MAX_VALUE, StatPatternsSizing.planAttempts(Long.MAX_VALUE, 1, .05, 30).attempts());
    }
    @Test void rareOutputDoesNotIteratePerAttempt() {
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            var result = StatPatternsSizing.planAttempts(1, 1e-12, .05, 30);
            assertTrue(result.attempts() > 2_900_000_000_000L);
            assertTrue(Math.pow(1 - 1e-12, result.attempts()) < .051);
        });
    }
    @Test void impossibleAmountFailsWithoutWrapping() {
        assertThrows(ArithmeticException.class, () -> StatPatternsSizing.planAttempts(1, 1e-30, .05, 30));
        assertThrows(ArithmeticException.class, () -> StatPatternsSizing.planAttempts(Long.MAX_VALUE, .1, .05, 30));
    }
    @ParameterizedTest @CsvSource({"31,.5", "1000,.8", "1000000000000,.01"})
    void stricterConfidenceAndLowerProbabilityNeedMoreInput(long target, double p) {
        long normal = StatPatternsSizing.planAttempts(target, p, .05, 30).attempts();
        assertTrue(StatPatternsSizing.planAttempts(target, p, .01, 30).attempts() > normal);
        assertTrue(StatPatternsSizing.planAttempts(target, p / 2, .05, 30).attempts() > normal);
    }
    @Test void rejectsInvalidInput() {
        for (double p : new double[] { 0, -1, Double.NaN, Double.POSITIVE_INFINITY, 1.1 })
            assertThrows(IllegalArgumentException.class, () -> StatPatternsSizing.planAttempts(1, p, .05, 30));
        for (double a : new double[] { 0, -1, Double.NaN, 1 })
            assertThrows(IllegalArgumentException.class, () -> StatPatternsSizing.planAttempts(1, .5, a, 30));
        assertThrows(IllegalArgumentException.class, () -> StatPatternsSizing.planAttempts(0, .5, .05, 30));
    }
}
