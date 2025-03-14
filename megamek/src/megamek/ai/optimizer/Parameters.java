/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package megamek.ai.optimizer;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Represents the parameters of a behavior function.
 * @author Luana Coppio
 */
public record Parameters(double[] params) {
    public Parameters subtract(Parameters other) {
        Parameters newParams = new Parameters(new double[params.length]);
        for (int i = 0; i < params.length; i++) {
            newParams.params[i] = params[i] - other.params[i];
        }
        return newParams;
    }

    public double maxAbs() {
        return Arrays.stream(params).map(Math::abs).max().orElse(0);
    }

    public static Parameters zeroes(int n) {
        return new Parameters(new double[n]);
    }

    public static Parameters random(int n, Random rand, double min, double max) {
        Parameters newParams = new Parameters(new double[n]);
        for (int i = 0; i < n; i++) {
            newParams.params[i] = rand.nextGaussian() * (max - min) + min;
        }
        return newParams;
    }

    public Parameters multiply(double scalar) {
        Parameters newParams = new Parameters(new double[params.length]);
        for (int i = 0; i < params.length; i++) {
            newParams.params[i] = params[i] * scalar;
        }
        return newParams;
    }

    public Parameters clamp(double min, double max, int from, int to) {
        assert from >= 0 && to <= params.length;
        Parameters newParams = new Parameters(new double[params.length]);
        for (int i = from; i < to; i++) {
            newParams.params[i] = clamp(params[i], min, max);
        }
        return newParams;
    }

    public Parameters clamp(double min, double max) {
        Parameters newParams = new Parameters(new double[params.length]);
        for (int i = 0; i < params.length; i++) {
            newParams.params[i] = clamp(params[i], min, max);
        }
        return newParams;
    }

    public double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public Parameters perturb(int index, double epsilon) {
        Parameters newParams = new Parameters(Arrays.copyOf(params, params.length));
        newParams.params[index] += epsilon;
        return newParams;
    }

    public void set(int index, double value) {
        params[index] = value;
    }

    public double get(ModelParameter param) {
        if (param.ordinal() >= params.length) {
            throw new IllegalArgumentException("Parameter index out of bounds: " + param.ordinal());
        }
        return params[param.ordinal()];
    }

    public double get(int index) {
        return params[index];
    }

    public int size() {
        return params.length;
    }

    public Parameters addNoise(double strength, Random random) {
        Parameters newParams = new Parameters(Arrays.copyOf(params, params.length));
        for (int i = 0; i < params.length; i++) {
            newParams.params[i] += random.nextGaussian() * strength;
        }
        return newParams;
    }

    public double[] toArray() {
        return Arrays.copyOf(params, params.length);
    }

    public Stream<Double> stream() {
        return Arrays.stream(toArray()).boxed();
    }

    public static Parameters fromArray(double[] arr) {
        return new Parameters(Arrays.copyOf(arr, arr.length));
    }

    public Parameters add(Parameters other) {
        Parameters newParams = new Parameters(new double[params.length]);
        for (int i = 0; i < params.length; i++) {
            newParams.params[i] = params[i] + other.params[i];
        }
        return newParams;
    }

    public Parameters copy() {
        return new Parameters(Arrays.copyOf(params, params.length));
    }
}
