/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.client.ui.util;

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
