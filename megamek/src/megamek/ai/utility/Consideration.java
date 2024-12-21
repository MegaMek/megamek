/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.ai.utility;

import java.util.HashMap;
import java.util.Map;

public abstract class Consideration {
    private Curve curve;
    private Map<String, Object> parameters;

    protected Consideration(Curve curve) {
        this(curve, new HashMap<>());
    }

    protected Consideration(Curve curve, Map<String, Object> parameters) {
        this.curve = curve;
        this.parameters = Map.copyOf(parameters);
    }

    public abstract double score(DecisionContext context);

    public Curve getCurve() {
        return curve;
    }

    public void setCurve(Curve curve) {
        this.curve = curve;
    }

    public Map<String, Object> getParameters() {
        return Map.copyOf(parameters);
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = Map.copyOf(parameters);
    }

    protected double getDoubleParameter(String key) {
        return (double) parameters.get(key);
    }

    protected int getIntParameter(String key) {
        return (int) parameters.get(key);
    }

    protected boolean getBooleanParameter(String key) {
        return (boolean) parameters.get(key);
    }

    protected String getStringParameter(String key) {
        return (String) parameters.get(key);
    }

    protected float getFloatParameter(String key) {
        return (float) parameters.get(key);
    }

    protected long getLongParameter(String key) {
        return (long) parameters.get(key);
    }

    public double computeResponseCurve(double score) {
        return curve.evaluate(score);
    }
}
