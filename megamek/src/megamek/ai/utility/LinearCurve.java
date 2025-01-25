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

import java.awt.*;
import java.util.StringJoiner;

import static megamek.codeUtilities.MathUtility.clamp01;

@JsonTypeName("LinearCurve")
public class LinearCurve implements Curve {
    private double m;
    private double b;

    @JsonCreator
    public LinearCurve(
        @JsonProperty("m") double m,
        @JsonProperty("b") double b) {
        this.m = m;
        this.b = b;
    }

    @Override
    public LinearCurve copy() {
        return new LinearCurve(m, b);
    }

    @Override
    public double evaluate(double x) {
        return clamp01(m * x + b);
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
        return new StringJoiner(", ", LinearCurve.class.getSimpleName() + " [", "]")
            .add("m=" + m)
            .add("b=" + b)
            .toString();
    }
}
