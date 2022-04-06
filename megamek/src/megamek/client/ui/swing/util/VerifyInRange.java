/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.ui.swing.util;

import megamek.common.util.StringUtil;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 3/18/14 1:14 PM
 */
public class VerifyInRange implements DataVerifier {

    private final double min;
    private final double max;
    private final boolean mustBeInteger;

    public VerifyInRange(double min, double max, boolean mustBeInteger) {
        if (min > max) {
            throw new IllegalArgumentException("Min (" + min + " cannot be greater than max (" + max + ").");
        }

        this.min = min;
        this.max = max;
        this.mustBeInteger = mustBeInteger;
    }

    @Override
    public String verify(Object value) {
        if (value instanceof Integer) {
            return verify((Integer) value);
        }
        if (value instanceof Double) {
            return verify((Double) value);
        }
        if (value instanceof String) {
            return verify((String) value);
        }

        return value + " is not a number.";
    }

    public String verify(String value) {
        if (StringUtil.isNumeric(value)) {
            return verify(Double.parseDouble(value));
        }
        return value + " is not a number.";
    }

    public String verify(Integer value) {
        return verify(Double.valueOf(value));
    }

    public String verify(Double value) {
        if (mustBeInteger && value != value.intValue()) {
            return value + " is not a whole number.";
        }

        if (value < min) {
            return value + " is below the minimum value of " + min;
        }
        if (value > max) {
            return value + " is exceeds the maximum value of " + max;
        }

        return null;
    }
}
