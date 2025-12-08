/*
 * Copyright (c) 2003-2004 - Ben Mazur (bmazur@sev.org).
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

package megamek.common.bays;

import java.io.Serial;

import megamek.common.units.Entity;

/**
 * Represents a volume of space set aside for carrying crew or passengers in more luxury than standard quarters.
 */
public final class FirstClassQuartersCargoBay extends Bay {
    @Serial
    private static final long serialVersionUID = 4161027191694822726L;

    private double weight = 0;

    /**
     * The default constructor is only for serialization.
     */
    private FirstClassQuartersCargoBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a space for the given tonnage of troops. For this class, only the weight of the troops (and their
     * equipment) are considered; if you'd like to think that they are stacked like lumber, be my guest.
     *
     * @param weight The weight of troops (in tons) this space can carry.
     */
    public FirstClassQuartersCargoBay(double weight, int doors) {
        totalSpace = Math.floor(weight / 10);
        this.weight = weight;
        currentSpace = Math.floor(weight / 10);
        this.minDoors = 0;
        this.doors = doors;
        currentDoors = doors;
    }

    /**
     * Create space for certain number of crew/passengers
     *
     * @param space The number of crew or passengers to accommodate
     */
    public FirstClassQuartersCargoBay(int space) {
        this(space * 10, 0);
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be of the appropriate type or there may be
     * no room for the unit.
     *
     * @param unit - the <code>Entity</code> to be loaded.
     *
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *       otherwise.
     */
    @Override
    public boolean canLoad(Entity unit) {
        // Assume that we cannot carry the unit.
        return false;
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        return "1st Class Quarters (" + getCurrentDoors() + " doors) - " + (int) currentSpace;
    }

    @Override
    public String getTransporterType() {
        return "1st Class Quarters";
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public boolean isQuarters() {
        return true;
    }

    @Override
    public String toString() {
        String bayType = "1stclassquarters";
        return this.bayString(
              bayType,
              weight,
              doors
        );
    }

    @Override
    public long getCost() {
        return 30000L * (long) totalSpace;
    }
}
