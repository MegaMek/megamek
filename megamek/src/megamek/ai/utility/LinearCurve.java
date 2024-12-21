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

import static megamek.codeUtilities.MathUtility.clamp01;

public class LinearCurve implements Curve {
    private final double m;
    private final double b;

    public LinearCurve(double m, double b) {
        this.m = m;
        this.b = b;
    }

    public double evaluate(double x) {
        return clamp01(m * x + b);
    }
}
