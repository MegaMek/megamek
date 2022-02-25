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

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 3/14/14 1:19 PM
 */
public class VerifyNotNull implements DataVerifier {

    @Override
    public String verify(Object value) {
        if (value != null) {
            return null;
        }
        return "Value is NULL.";
    }
}
