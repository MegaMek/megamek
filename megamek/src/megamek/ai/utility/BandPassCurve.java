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

@JsonTypeName("BandPassCurve")
public class BandPassCurve implements Curve {
    private double m;
    private double b;
    private double k;
    private double c;

    @JsonCreator
    public BandPassCurve(
        @JsonProperty("m") double m,
        @JsonProperty("b") double b,
        @JsonProperty("k") double k,
        @JsonProperty("c") double c) {
        this.m = m;
        this.b = b;
        this.k = k;
        this.c = c;
    }

    @Override
    public BandPassCurve copy() {
        return new BandPassCurve(m, b, k, c);
    }

    @Override
    public double evaluate(double x) {
        var bandStart = m - b / 2;
        var bandEnd = m + b / 2;

        return clamp01(x < bandStart ? 0d + k : x > bandEnd ? 0d + k : 1d + c);
    }

    @Override
    public double getK() {
        return k;
    }

    @Override
    public void setK(double k) {
        this.k = k;
    }

    @Override
    public double getC() {
        return c;
    }

    @Override
    public void setC(double c) {
        this.c = c;
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
    public void setM(double m) {
        this.m = m;
    }

    @Override
    public void setB(double b) {
        this.b = b;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BandPassCurve.class.getSimpleName() + " [", "]")
            .add("m=" + m)
            .add("b=" + b)
            .add("k=" + k)
            .add("c=" + c)
            .toString();
    }
}
