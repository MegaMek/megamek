package megamek.common.rules.totalwarfare;
/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.equipment.WeaponType;
import megamek.common.rules.RulesUnderwater;

public class TWRulesUnderwater extends RulesUnderwater {
    /**
     * What is the target number for the breach?
     *
     * @return the breach target number is 10
     */
    @Override
    public int getBreachTarget() {
        return 10;
    }

    /**
     * Crossing the waterline blocks LOS
     *
     * @return true if water blocks line of sight
     */
    @Override
    public boolean waterBlocksLOS() {
        return true;
    }

    /**
     * Get the short range of the weapon in water
     * @param weaponType
     * @return short range
     */
    @Override
    public int getShortRange(WeaponType weaponType) {
        return weaponType.getWShortRange();
    }

    /**
     * Get the medium range of the weapon in water
     * @param weaponType
     * @return medium range
     */
    @Override
    public int getMediumRange(WeaponType weaponType) {
        return weaponType.getWMediumRange();
    }

    /**
     * Get the long range of the weapon in water
     * @param weaponType
     * @return long range
     */
    @Override
    public int getLongRange(WeaponType weaponType) {
        return weaponType.getWLongRange();
    }

    /**
     * Get the extreme range of the weapon in water
     * @param weaponType
     * @return extreme range
     */
    @Override
    public int getExtremeRange(WeaponType weaponType) {
        return weaponType.getWExtremeRange();
    }
}
