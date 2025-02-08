/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */
package megamek.utilities.ai;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Represents the parameters of a behavior function.
 * @author Luana Coppio
 */
public record BehaviorParameters(
    double p1, double p2, double p3, double p4, double p5,
    double p6, double p7, double p8, double p9, double p10, double p11, double p12, double p13, double p14,
    double p15, double p16, double p17, double p18, double p19,
    double p20, double p21, double p22, double p23, double p24,
    double p25, double p26, double p27, double p28, double p29
) {
    public BehaviorParameters subtract(BehaviorParameters other) {
        return new BehaviorParameters(
            p1 - other.p1, p2 - other.p2, p3 - other.p3,
            p4 - other.p4, p5 - other.p5, p6 - other.p6,
            p7 - other.p7, p8 - other.p8, p9 - other.p9,
            p10 - other.p10, p11 - other.p11, p12 - other.p12,
            p13 - other.p13, p14 - other.p14, p15 - other.p15,
            p16 - other.p16, p17 - other.p17, p18 - other.p18,
            p19 - other.p19, p20 - other.p20, p21 - other.p21,
            p22 - other.p22, p23 - other.p23, p24 - other.p24,
            p25 - other.p25, p26 - other.p26, p27 - other.p27,
            p28 - other.p28, p29 - other.p29
        );
    }

    public double maxAbs() {
        return Arrays.stream(toArray()).map(Math::abs).max().orElse(0);
    }

    public static BehaviorParameters random(Random rand) {
        BehaviorParameters behaviorParameters = new BehaviorParameters(
            rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(),
            rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(),
            rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(),
            rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(),
            rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(),
            rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(),
            rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(),
            rand.nextGaussian() * 1.5, rand.nextGaussian() * 1.5,
            rand.nextGaussian() * 1.5, rand.nextGaussian() * 1.5,
            rand.nextGaussian() * 1.5, rand.nextGaussian() * 1.5,
            rand.nextGaussian() * 1.5, rand.nextGaussian() * 1.5);
        return behaviorParameters.clamp(0, 1);
    }

    public BehaviorParameters multiply(double scalar) {
        return new BehaviorParameters(
            p1 * scalar, p2 * scalar, p3 * scalar,
            p4 * scalar, p5 * scalar, p6 * scalar,
            p7 * scalar, p8 * scalar, p9 * scalar,
            p10 * scalar, p11 * scalar, p12 * scalar,
            p13 * scalar, p14 * scalar, p15 * scalar,
            p16 * scalar, p17 * scalar, p18 * scalar,
            p19 * scalar, p20 * scalar, p21 * scalar,
            p22 * scalar, p23 * scalar, p24 * scalar,
            p25 * scalar, p26 * scalar, p27 * scalar,
            p28 * scalar, p29 * scalar
        );
    }

    public BehaviorParameters clamp(double min, double max) {
        return new BehaviorParameters(
            clamp(p1, min, max), clamp(p2, min, max), clamp(p3, min, max),
            clamp(p4, min, max), clamp(p5, min, max), clamp(p6, min, max),
            clamp(p7, min, max), clamp(p8, min, max), clamp(p9, min, max),
            clamp(p10, min, max), clamp(p11, min, max), clamp(p12, min, max),
            clamp(p13, min, max), clamp(p14, min, max), clamp(p15, min, max),
            clamp(p16, min, max), clamp(p17, min, max), clamp(p18, min, max),
            clamp(p19, min, max), clamp(p20, min, max), clamp(p21, min, max),
            clamp(p22, min, max*1.5), clamp(p23, min, max*1.5), clamp(p24, min, max*1.5),
            clamp(p25, min, max*1.5), clamp(p26, min, max*1.5), clamp(p27, min, max*1.5),
            clamp(p28, min, max*1.5), clamp(p29, min, max*1.5)
        );
    }

    public double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public BehaviorParameters perturb(int index, double epsilon) {
        double[] params = toArray();
        params[index] += epsilon;
        return fromArray(params);
    }

    public BehaviorParameters set(int index, double value) {
        double[] params = toArray();
        params[index] = value;
        return fromArray(params);
    }

    public double get(int index) {
        double[] params = toArray();
        return params[index];
    }

    public int size() {
        return 28;
    }

    public BehaviorParameters addNoise(double strength) {
        Random rand = new Random();
        return this.add(new BehaviorParameters(
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength, rand.nextGaussian()*strength,
            rand.nextGaussian()*strength
        ));
    }

    public double[] toArray() {
        return new double[]{p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21, p22,
            p23, p24, p25, p26, p27, p28, p29};
    }

    public Stream<Double> stream() {
        return Arrays.stream(toArray()).boxed();
    }

    public static BehaviorParameters fromArray(double[] arr) {
        return new BehaviorParameters(
            arr[0], arr[1], arr[2], arr[3], arr[4],
            arr[5], arr[6], arr[7], arr[8], arr[9],
            arr[10], arr[11], arr[12], arr[13], arr[14],
            arr[15], arr[16], arr[17], arr[18], arr[19],
            arr[20], arr[21], arr[22], arr[23], arr[24],
            arr[25], arr[26], arr[27], arr[28]
        );
    }

    public BehaviorParameters add(BehaviorParameters other) {
        return new BehaviorParameters(
            p1 + other.p1(), p2 + other.p2(), p3 + other.p3(),
            p4 + other.p4(), p5 + other.p5(), p6 + other.p6(),
            p7 + other.p7(), p8 + other.p8(), p9 + other.p9(),
            p10 + other.p10(), p11 + other.p11(), p12 + other.p12(),
            p13 + other.p13(), p14 + other.p14(), p15 + other.p15(),
            p16 + other.p16(), p17 + other.p17(), p18 + other.p18(),
            p19 + other.p19(), p20 + other.p20(), p21 + other.p21(),
            p22 + other.p22(), p23 + other.p23(), p24 + other.p24(),
            p25 + other.p25(), p26 + other.p26(), p27 + other.p27(),
            p28 + other.p28(), p29 + other.p29()
        );
    }

    public BehaviorParameters copy() {
        return new BehaviorParameters(
            p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14,
            p15, p16, p17, p18, p19, p20, p21, p22, p23, p24, p25, p26,
            p27, p28, p29
        );
    }
}
