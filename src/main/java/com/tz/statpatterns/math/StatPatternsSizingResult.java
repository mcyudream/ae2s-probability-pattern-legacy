
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

/**
 * The result of planning how many attempts are needed to reach a target number
 * of successes with a guaranteed confidence level.
 *
 * @param targetSuccesses the number of successful outputs required
 * @param attempts        the number of attempts that must be planned
 */
public record StatPatternsSizingResult(
        long targetSuccesses,
        long attempts) {
}
