/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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
 * @version %Id%
 * @since 3/14/14 3:44 PM
 */
public class VerifyIsPositiveInteger implements DataVerifier {

    private final DataVerifier notNullOrEmpty = new VerifyNotNullOrEmpty();

    @Override
    public String verify(Object value) {
        if (value instanceof Integer) {
            return verify((Integer) value);
        }
        if (value instanceof String) {
            return verify((String) value);
        }
        return value + " is wrong object type.  Should be text.";
    }

    public String verify(Integer value) {
        if (value < 0) {
            return value + " is not a positive integer.";
        }
        return null;
    }

    public String verify(String value) {
        String result = notNullOrEmpty.verify(value);
        if (result != null) {
            return result;
        }

        if (!StringUtil.isPositiveInteger(value)) {
            return value + " is not a positive integer.";
        }

        return null;
    }
}
