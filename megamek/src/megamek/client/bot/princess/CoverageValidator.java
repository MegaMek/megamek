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
package megamek.client.bot.princess;

import megamek.common.Coords;
import megamek.common.Entity;

/**
 * Validates the coverage of a unit in the battlefield.
 *
 * @author Luana Coppio
 */
public record CoverageValidator(Princess owner) {

    /**
     * Validates if the unit has coverage from at least one ally.
     *
     * @param unit        The unit to be validated.
     * @param newPosition The new position to be validated.
     *
     * @return TRUE if the unit has coverage from at least one ally.
     */
    public boolean validateUnitCoverage(Entity unit, Coords newPosition) {
        // Check 0.6x coverage from at least one ally
        return owner.getFriendEntities().stream()
              .filter(e -> !e.equals(unit))
              .anyMatch(ally -> ally.getPosition().distance(newPosition) <=
                    ally.getMaxWeaponRange() * 0.6);
    }

    /**
     * Indicates if the unit is exposed to enemy fire.
     *
     * @param unit The unit to be checked.
     *
     * @return TRUE if the unit is exposed to enemy fire.
     */
    public boolean isPositionExposed(Entity unit) {
        return !validateUnitCoverage(unit, unit.getPosition());
    }

}
