/*
 * Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.util.Collection;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 3/14/14 1:20 PM
 */
public class VerifyNotEmpty implements DataVerifier {

    private static final String EMPTY = "Value is empty.";

    @Override
    public String verify(Object value) {
        if (value instanceof String) {
            return verify((String) value);
        }
        if (value instanceof Collection<?>) {
            return verify((Collection<?>) value);
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

    public String verify(Collection<?> value) {
        if (!value.isEmpty()) {
            return null;
        }
        return EMPTY;
    }
}
