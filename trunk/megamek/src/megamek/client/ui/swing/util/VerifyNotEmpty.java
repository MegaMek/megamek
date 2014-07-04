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

import java.util.Collection;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %Id%
 * @since 3/14/14 1:20 PM
 */
public class VerifyNotEmpty implements DataVerifier {

    private static final String EMPTY = "Value is empty.";

    @Override
    public String verify(Object value) {
        if (value instanceof String) {
            return verify((String) value);
        }
        if (value instanceof Collection) {
            return verify((Collection) value);
        }
        if (value instanceof Object[]) {
            return verify((Object[]) value);
        }

        return value + " is invalid Object type.";
    }

    public String verify(String value) {
        if (!value.isEmpty()) {
            return null;
        }
        return EMPTY;
    }

    public String verify(Object[] value) {
        if (value.length > 0) {
            return null;
        }
        return EMPTY;
    }

    public String verify(Collection value) {
        if (!value.isEmpty()) {
            return null;
        }
        return EMPTY;
    }
}
