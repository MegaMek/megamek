/*
 * Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.util.verifier;

import megamek.common.util.StringUtil;

/**
 * @author arlith
 * @since 10/3/14
 */
public class VerifyIsInteger implements DataVerifier {

    private final DataVerifier notNullOrEmpty = new VerifyNotNullOrEmpty();

    @Override
    public String verify(Object value) {
        if (value instanceof Integer) {
            return null;
        }

        if (value instanceof String stringValue) {
            return verify(stringValue);
        }

        return value + " is wrong object type.  Should be text.";
    }

    public String verify(String value) {
        String result = notNullOrEmpty.verify(value);
        if (result != null) {
            return result;
        }

        if (!StringUtil.isInteger(value)) {
            return value + " is not an integer.";
        }

        return null;
    }
}
