
/*
 * Probability Pattern for AE2
 * Copyright (C) 2026 TaoLe-si
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tz.statpatterns.math;

public final class StatPatternsSizing {
    private StatPatternsSizing() {
    }

    public static StatPatternsSizingResult planAttempts(long targetSuccesses, double successProbability,
            double alpha, int smallSampleLimit) {
        validate(targetSuccesses, successProbability, alpha);
        if (smallSampleLimit < 1 || smallSampleLimit > 30) throw new IllegalArgumentException("Exact limit must be 1..30");

        if (successProbability == 1.0) {
            return new StatPatternsSizingResult(targetSuccesses, targetSuccesses);
        }

        if (targetSuccesses <= smallSampleLimit) {
            return exactBinomialPlan(targetSuccesses, successProbability, alpha);
        }

        return normalApproximationPlan(targetSuccesses, successProbability, alpha);
    }

    private static StatPatternsSizingResult exactBinomialPlan(long target, double p, double alpha) {
        return search(target, n -> binomialLowerTail(n, p, target - 1) <= alpha);
    }

    private static StatPatternsSizingResult normalApproximationPlan(long target, double p, double alpha) {
        double z = -inverseStandardNormal(alpha);
        return search(target, n -> normalZ(target, n, p) >= z);
    }

    private static StatPatternsSizingResult search(long target, java.util.function.LongPredicate sufficient) {
        if (!sufficient.test(Long.MAX_VALUE))
            throw new ArithmeticException("Required attempts exceed signed 64-bit range");
        long low = target, high = Long.MAX_VALUE;
        while (low < high) {
            long mid = low + (high - low) / 2;
            if (sufficient.test(mid)) high = mid;
            else low = mid + 1;
        }
        return new StatPatternsSizingResult(target, low);
    }

    // Log-space summation avoids q^n underflow for rare outputs and near-certain p.
    static double binomialLowerTail(long attempts, double p, long maxSuccesses) {
        if (maxSuccesses < 0) return 0;
        if (maxSuccesses >= attempts) return 1;
        if (p == 1) return 0;
        double logTerm = attempts * Math.log1p(-p), logSum = logTerm;
        double logOdds = Math.log(p) - Math.log1p(-p);
        for (long k = 1; k <= maxSuccesses; k++) {
            logTerm += Math.log((double) (attempts - k + 1)) - Math.log(k) + logOdds;
            double larger = Math.max(logSum, logTerm);
            logSum = larger + Math.log1p(Math.exp(Math.min(logSum, logTerm) - larger));
        }
        return Math.min(1, Math.exp(logSum));
    }

    private static double normalZ(long target, long attempts, double p) {
        double mean = attempts * p;
        return (mean - target) / Math.sqrt(mean * (1 - p));
    }

    private static double inverseStandardNormal(double probability) {
        if (!(probability > 0.0 && probability < 1.0)) {
            throw new IllegalArgumentException("Probability must be in (0, 1).");
        }

        var a = new double[] {
                -3.969683028665376e+01, 2.209460984245205e+02,
                -2.759285104469687e+02, 1.383577518672690e+02,
                -3.066479806614716e+01, 2.506628277459239e+00
        };
        var b = new double[] {
                -5.447609879822406e+01, 1.615858368580409e+02,
                -1.556989798598866e+02, 6.680131188771972e+01,
                -1.328068155288572e+01
        };
        var c = new double[] {
                -7.784894002430293e-03, -3.223964580411365e-01,
                -2.400758277161838e+00, -2.549732539343734e+00,
                4.374664141464968e+00, 2.938163982698783e+00
        };
        var d = new double[] {
                7.784695709041462e-03, 3.224671290700398e-01,
                2.445134137142996e+00, 3.754408661907416e+00
        };

        var low = 0.02425;
        var high = 1.0 - low;
        double q;
        double r;

        if (probability < low) {
            q = Math.sqrt(-2.0 * Math.log(probability));
            return (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
                    / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0);
        }
        if (probability > high) {
            q = Math.sqrt(-2.0 * Math.log(1.0 - probability));
            return -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
                    / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0);
        }

        q = probability - 0.5;
        r = q * q;
        return (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q
                / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1.0);
    }

    public static void validate(long targetSuccesses, double successProbability, double alpha) {
        if (targetSuccesses <= 0) {
            throw new IllegalArgumentException("Target successes must be positive.");
        }
        if (!(successProbability > 0.0 && successProbability <= 1.0)) {
            throw new IllegalArgumentException("Success probability must be in (0, 1].");
        }
        if (!(alpha > 0.0 && alpha < 1.0)) {
            throw new IllegalArgumentException("Alpha must be in (0, 1).");
        }
    }
}
