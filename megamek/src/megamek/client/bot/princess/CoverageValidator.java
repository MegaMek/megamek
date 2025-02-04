/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */
package megamek.client.bot.princess;

import megamek.common.Coords;
import megamek.common.Entity;

/**
 * Validates the coverage of a unit in the battlefield.
 * @author Luana Coppio
 */
public class CoverageValidator {

    private final Princess owner;

    public CoverageValidator(Princess owner) {
        this.owner = owner;
    }

    /**
     * Validates if the unit has coverage from at least one ally.
     * @param unit  The unit to be validated.
     * @param newPosition  The new position to be validated.
     * @return  TRUE if the unit has coverage from at least one ally.
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
     * @param unit The unit to be checked.
     * @return  TRUE if the unit is exposed to enemy fire.
     */
    public boolean isPositionExposed(Entity unit) {
        return !validateUnitCoverage(unit, unit.getPosition());
    }

}
