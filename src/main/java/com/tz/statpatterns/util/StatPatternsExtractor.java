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
package com.tz.statpatterns.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Optional;



/**
 * Best-effort reflection based extraction of a success probability from an
 * arbitrary recipe object (JEI/EMI/REI recipe wrappers or their underlying
 * vanilla recipes).
 * <p>
 * Recipes that expose the probability through a well-known getter (e.g.
 * {@code successProbability()}, {@code getChance()}) or a {@code probability} /
 * {@code chance} field are supported. Values given as a percentage in (1, 100]
 * are normalized to the (0, 1] range.
 * <p>
 * This utility intentionally depends only on JDK classes plus recipe wrappers,
 * so it can be safely referenced from every recipe-viewer integration regardless
 * of which viewers are installed at runtime.
 */
public final class StatPatternsExtractor {
    private static final int MAX_DEPTH = 2;

    private StatPatternsExtractor() {
    }

    /**
     * Extract a normalized success probability from the given object, unwrapping
     * recipe wrappers wrappers first.
     *
     * @return the probability in (0, 1], or {@link Optional#empty()} if none found.
     */
    public static Optional<Double> extract(Object value) {
        return extract(value, 0);
    }

    private static Optional<Double> extract(Object value, int depth) {
        if (value == null || depth > MAX_DEPTH) {
            return Optional.empty();
        }
        if (value instanceof Number number) {
            return normalize(number.doubleValue());
        }

        for (var methodName : List.of("successProbability", "getSuccessProbability", "probability",
                "getProbability", "chance", "getChance")) {
            try {
                Method method = value.getClass().getMethod(methodName);
                if (method.getParameterCount() == 0 && Number.class.isAssignableFrom(wrap(method.getReturnType()))) {
                    return normalize(((Number) method.invoke(value)).doubleValue());
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        for (var field : value.getClass().getDeclaredFields()) {
            var name = field.getName().toLowerCase(Locale.ROOT);
            if (name.contains("probability") || name.contains("chance")) {
                var found = readField(value, field, depth);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        for (var getter : List.of("getRecipe", "getInternalRecipe")) {
            try {
                var nested = value.getClass().getMethod(getter).invoke(value);
                var found = extract(nested, depth + 1);
                if (found.isPresent()) return found;
            } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
        for (var field : value.getClass().getDeclaredFields()) {
            if (field.getName().equalsIgnoreCase("recipe")) {
                var found = readField(value, field, depth);
                if (found.isPresent()) return found;
            }
        }
        return Optional.empty();
    }

    private static Optional<Double> readField(Object owner, Field field, int depth) {
        try {
            field.setAccessible(true);
            var fieldValue = field.get(owner);
            return extract(fieldValue, depth + 1);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Double> normalize(double probability) {
        if (probability > 1.0 && probability <= 100.0) {
            probability /= 100.0;
        }
        if (probability > 0.0 && probability <= 1.0) {
            return Optional.of(probability);
        }
        return Optional.empty();
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        return type;
    }
}
