/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.ai.utility;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.StringJoiner;

import static megamek.codeUtilities.MathUtility.clamp01;

@JsonTypeName("ParabolicCurve")
public class ParabolicCurve implements Curve {
    private double m;
    private double b;
    private double k;

    @JsonCreator
    public ParabolicCurve(
        @JsonProperty("m") double m,
        @JsonProperty("b") double b,
        @JsonProperty("k") double k) {
        this.m = m;
        this.b = b;
        this.k = k;
    }

    @Override
    public ParabolicCurve copy() {
        return new ParabolicCurve(m, b, k);
    }

    @Override
    public double evaluate(double x) {
        return clamp01(-m * Math.pow(x - b, 2) + k);
    }

    @Override
    public double getM() {
        return m;
    }

    @Override
    public double getB() {
        return b;
    }

    @Override
    public double getK() {
        return k;
    }

    @Override
    public void setM(double m) {
        this.m = m;
    }

    @Override
    public void setB(double b) {
        this.b = b;
    }

    @Override
    public void setK(double k) {
        this.k = k;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ParabolicCurve.class.getSimpleName() + "[", "]")
            .add("m=" + m)
            .add("b=" + b)
            .add("k=" + k)
            .toString();
    }
}
