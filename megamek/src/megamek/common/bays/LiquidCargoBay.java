/*
  Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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
 * Represents a volume of space set aside for carrying liquid cargo
 */
public final class LiquidCargoBay extends Bay {
    @Serial
    private static final long serialVersionUID = 4161027191694822726L;

    private double weight = 0;

    /**
     * The default constructor is only for serialization.
     */
    private LiquidCargoBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a space for the given tonnage of troops. For this class, only the weight of the troops (and their
     * equipment) are considered; if you'd like to think that they are stacked like lumber, be my guest.
     *
     * @param space The weight of troops (in tons) this space can carry.
     */
    public LiquidCargoBay(double space, int doors, int bayNumber) {
        totalSpace = space;
        weight = space / 0.91;
        currentSpace = space;
        this.doors = doors;
        this.bayNumber = bayNumber;
        currentDoors = doors;
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
        StringBuilder returnString = new StringBuilder("Liquid Cargo Space " + numDoorsString() + " - ");

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
        return "Liquid Cargo";
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        String bayType = "LiquidCargoBay";
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

    @Override
    public long getCost() {
        // Based on the weight of the equipment (not capacity), rounded up to the whole ton
        return 100L * (long) Math.ceil(getWeight());
    }
}
