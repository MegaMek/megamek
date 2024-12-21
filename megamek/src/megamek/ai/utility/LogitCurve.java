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

public class LogitCurve implements Curve {
    private final double m;
    private final double b;
    private final double k;
    private final double c;

    public LogitCurve(double m, double b, double k, double c) {
        this.m = m;
        this.b = b;
        this.k = k;
        this.c = c;
    }

    public double evaluate(double x) {
        // Ensure p is in the valid range
        // Typically, you want 0 < p < 1 to avoid division by zero or log of zero.
        if (x - c == 0) {
            return 0d;
        }
        return clamp01(b - (1.0 / k) * Math.log((m - (x - c)) / (x - c)));
    }
}
