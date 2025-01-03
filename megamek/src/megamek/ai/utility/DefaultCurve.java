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


public enum DefaultCurve {
    Linear(new LinearCurve(1.0, 0)),
    LinearDecreasing(new LinearCurve(-1.0, 1)),

    Parabolic(new ParabolicCurve(4.0, 0.5, 1.0)),
    ParabolicNegative(new ParabolicCurve(-4.0, 0.5, 0.0)),

    Logistic(new LogisticCurve(1.0, 0.5, 10.0, 0.0)),
    LogisticDecreasing(new LogisticCurve(1.0, 0.5, -10.0, 0.0)),

    Logit(new LogitCurve(1.0, 0.5, -15.0, 0.0)),
    LogitDecreasing(new LogitCurve(1.0, 0.5, 15.0, 0.0)),

    BandPass(new BandPassCurve(0.5, 0.2, 0, 0)),
    BandFilter(new BandFilterCurve(0.5, 0.2, 0, 0));

    private final Curve curve;

    DefaultCurve(Curve curve) {
        this.curve = curve;
    }

    public static DefaultCurve fromCurve(Curve curve) {
        if (curve instanceof LinearCurve) {
            return Linear;
        } else if (curve instanceof ParabolicCurve) {
            return Parabolic;
        } else if (curve instanceof LogisticCurve) {
            return Logistic;
        } else if (curve instanceof LogitCurve) {
            return Logit;
        } else if (curve instanceof BandPassCurve) {
            return BandPass;
        } else if (curve instanceof BandFilterCurve) {
            return BandFilter;
        }
        // Return Linear as default
        return Linear;
    }

    public Curve getCurve() {
        return curve.copy();
    }
}
