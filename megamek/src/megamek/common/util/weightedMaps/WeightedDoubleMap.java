/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.util.weightedMaps;

import megamek.common.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Constructs a table of values each with a double weight that makes them more or less likely to be
 * selected at random
 *
 * @param <T> The values in the table
 */
public class WeightedDoubleMap<T> extends AbstractWeightedMap<Double, T> {
    //region Variable Declarations
    @Serial
    private static final long serialVersionUID = -2040584639476035520L;
    //endregion Variable Declarations

    @Override
    public @Nullable T add(final Double weight, final T value) {
        return (weight > 0.0d) ? put(isEmpty() ? weight : lastKey() + weight, value) : null;
    }

    @Override
    public @Nullable T randomItem() {
        return isEmpty() ? null : randomItem(getRandomKey());
    }

    private double getRandomKey() {
        return ThreadLocalRandom.current().nextDouble(0.0d, lastKey());
    }

    /**
     * @return an optional random item from the weighted map, or Optional empty if it is empty
     */
    public Optional<T> randomOptionalItem(T[] except) {
        double randomKey = getRandomKey();
        double cumulativeWeight = 0.0;
        var isException = false;
        var i = 0;
        for (var entry : entrySet()) {
            isException = false;
            for (i = 0; i < except.length; i++) {
                if (entry.getValue().equals(except[i])) {
                    isException = true;
                    break;
                }
            }
            if (isException) {
                continue;
            }
            cumulativeWeight += entry.getKey();
            if (randomKey <= cumulativeWeight) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }

    public WeightedDoubleMap() {}

    WeightedDoubleMap(Object... input) {
        if ((input.length & 1) != 0) { // implicit null check of input
            throw new InternalError("length is odd");
        }
        for (int i = 0; i < input.length; i += 2) {
            @SuppressWarnings("unchecked")
            T t = Objects.requireNonNull((T) input[i]);
            double weight = (double) input[i+1];
            var alreadyPresent = this.entrySet().stream().anyMatch(entry -> entry.getValue().equals(t));
            if (alreadyPresent) {
                throw new IllegalArgumentException("duplicate value: " + t);
            }
            this.put(weight, t);
        }
    }

    public static <T> WeightedDoubleMap<T> of(T t1, double w1) {
        return new WeightedDoubleMap<>(t1, w1);
    }

    public static <T> WeightedDoubleMap<T> of(T t1, double w1, T t2, double w2) {
        return new WeightedDoubleMap<>(t1, w1, t2, w2);
    }

    public static <T> WeightedDoubleMap<T> of(T t1, double w1, T t2, double w2, T t3, double w3) {
        return new WeightedDoubleMap<>(t1, w1, t2, w2, t3, w3);
    }

    public static <T> WeightedDoubleMap<T> of(T t1, double w1, T t2, double w2, T t3, double w3, T t4, double w4) {
        return new WeightedDoubleMap<>(t1, w1, t2, w2, t3, w3, t4, w4);
    }

    public static <T> WeightedDoubleMap<T> of(T t1, double w1, T t2, double w2, T t3, double w3, T t4, double w4, T t5, double w5) {
        return new WeightedDoubleMap<>(t1, w1, t2, w2, t3, w3, t4, w4, t5, w5);
    }

    public static <T> WeightedDoubleMap<T> of(T t1, double w1, T t2, double w2, T t3, double w3, T t4, double w4, T t5, double w5, T t6, double w6) {
        return new WeightedDoubleMap<>(t1, w1, t2, w2, t3, w3, t4, w4, t5, w5, t6, w6);
    }

    public static <T> WeightedDoubleMap<T> of(T t1, double w1, T t2, double w2, T t3, double w3, T t4, double w4, T t5, double w5, T t6, double w6, T t7, double w7) {
        return new WeightedDoubleMap<>(t1, w1, t2, w2, t3, w3, t4, w4, t5, w5, t6, w6, t7, w7);
    }

    public static <T> WeightedDoubleMap<T> of(T t1, double w1, T t2, double w2, T t3, double w3, T t4, double w4, T t5, double w5, T t6, double w6, T t7, double w7, T t8, double w8) {
        return new WeightedDoubleMap<>(t1, w1, t2, w2, t3, w3, t4, w4, t5, w5, t6, w6, t7, w7, t8, w8);
    }

    public static <T> WeightedDoubleMap<T> of(T t1, double w1, T t2, double w2, T t3, double w3, T t4, double w4, T t5, double w5, T t6, double w6, T t7, double w7, T t8, double w8, T t9, double w9) {
        return new WeightedDoubleMap<>(t1, w1, t2, w2, t3, w3, t4, w4, t5, w5, t6, w6, t7, w7, t8, w8, t9, w9);
    }

    public static <T> WeightedDoubleMap<T> of(T t1, double w1, T t2, double w2, T t3, double w3, T t4, double w4, T t5, double w5, T t6, double w6, T t7, double w7, T t8, double w8, T t9, double w9, T t10, double w10) {
        return new WeightedDoubleMap<>(t1, w1, t2, w2, t3, w3, t4, w4, t5, w5, t6, w6, t7, w7, t8, w8, t9, w9, t10, w10);
    }
}
