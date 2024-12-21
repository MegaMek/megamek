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

public class LogisticCurve implements Curve {
    private final double m;
    private final double b;
    private final double k;
    private final double c;

    public LogisticCurve(double m, double b, double k, double c) {
        this.m = m;
        this.b = b;
        this.k = k;
        this.c = c;
    }

    public double evaluate(double x) {
        return clamp01(m / (1 + Math.exp(-k * (x - b))) + c);

    }
}
