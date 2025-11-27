/*
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
package megamek.common.enums;

/**
 * Represents the targeting mode for the Variable Range Targeting quirk.
 * <p>
 * Per BMM pg. 86: - Player selects Long or Short mode during End Phase for the NEXT turn - LONG mode:
 * -1 TN at long range, +1 TN at short range - SHORT mode: -1 TN at short range, +1 TN at long range - Medium range is
 * unaffected by either mode
 */
public enum VariableRangeTargetingMode {
    //region Enum Declarations
    /** Long range preference: -1 to long range attacks, +1 to short range attacks */
    LONG,
    /** Short range preference: -1 to short range attacks, +1 to long range attacks */
    SHORT;
    //endregion Enum Declarations

    //region Boolean Comparison Methods
    public boolean isLong() {
        return this == LONG;
    }

    public boolean isShort() {
        return this == SHORT;
    }
    //endregion Boolean Comparison Methods

    /**
     * Returns the default targeting mode (LONG).
     *
     * @return the default VariableRangeTargetingMode
     */
    public static VariableRangeTargetingMode defaultMode() {
        return LONG;
    }

    /**
     * Returns the short range modifier for this mode.
     *
     * @return -1 for SHORT mode (bonus), +1 for LONG mode (penalty)
     */
    public int getShortRangeModifier() {
        return isShort() ? -1 : 1;
    }

    /**
     * Returns the long range modifier for this mode.
     *
     * @return -1 for LONG mode (bonus), +1 for SHORT mode (penalty)
     */
    public int getLongRangeModifier() {
        return isLong() ? -1 : 1;
    }
}
