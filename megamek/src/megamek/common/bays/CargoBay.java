/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.bays;

import java.io.Serial;

import megamek.common.units.Entity;
import megamek.common.units.InfantryTransporter;

/**
 * Represents a volume of space set aside for carrying general cargo aboard large spacecraft and mobile structures.
 */
public final class CargoBay extends Bay implements InfantryTransporter {
    @Serial
    private static final long serialVersionUID = 4161027191694822726L;

    /**
     * The default constructor is only for serialization.
     */
    private CargoBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a space for the given tonnage of troops. For this class, only the weight of the troops (and their
     * equipment) are considered; if you'd like to think that they are stacked like lumber, be my guest.
     *
     * @param space The weight of troops (in tons) this space can carry.
     */
    public CargoBay(double space, int doors, int bayNumber) {
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        this.bayNumber = bayNumber;
        currentDoors = doors;
    }

    @Override
    public boolean canLoad(Entity unit) {
        // Infantry is only restricted by adjacency requirements (TW pp. 223 - 225)
        return unit.isInfantry();
    }

    @Override
    public boolean canUnloadUnits() {
        // Infantry is only restricted by adjacency requirements (TW pp. 223 - 225)
        return super.canUnloadUnits() || troops.stream()
              .map(unit -> game.getEntity(unit))
              .anyMatch(e -> (e != null && e.isInfantry()));
    }

    // Use the calculated original weight for infantry in cargo bays
    @Override
    public double spaceForUnit(Entity unit) {
        return Math.round(unit.getWeight() / unit.getInternalRemainingPercent());
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        StringBuilder returnString = new StringBuilder("Cargo Space "
              + numDoorsString() + " - ");

        if (getUnused() != Math.round(getUnused())) {
            returnString.append(String.format("%1$,.3f", getUnused()));
        } else {
            returnString.append(String.format("%1$,.0f", getUnused()));
        }

        returnString.append(" tons");
        return returnString.toString();
    }

    @Override
    public String getTransporterType() {
        return "Cargo";
    }

    @Override
    public String toString() {
        String bayType = "cargobay";
        return this.bayString(
              bayType,
              totalSpace,
              doors,
              bayNumber
        );
    }

    @Override
    public boolean isCargo() {
        return true;
    }
}
