/*
 * Copyright (c) 2003-2004 - Ben Mazur (bmazur@sev.org).
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
 * Represents a standard support vehicle crew seat.
 */
public class StandardSeatCargoBay extends Bay {
    @Serial
    private static final long serialVersionUID = 4161027191694822726L;

    protected double weight = 0;

    /**
     * The default constructor is only for serialization.
     */
    protected StandardSeatCargoBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Creates standard crew seating for support vehicles.
     *
     * @param space The number of seats
     */
    public StandardSeatCargoBay(double space) {
        totalSpace = currentSpace = space;
        weight = space * 0.075;
        doors = currentDoors = 0;
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be of the appropriate type or there may be
     * no room for the unit.
     *
     * @param unit the <code>Entity</code> to be loaded.
     *
     * @return <code>true</code> if the unit can be loaded, <code>false</code> otherwise.
     */
    @Override
    public boolean canLoad(Entity unit) {
        return false;
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        return "Seating (Standard) - " + currentSpace;
    }

    @Override
    public String getTransporterType() {
        return "Standard Seats";
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
        String bayType = "standardseats";
        return this.bayString(
              bayType,
              currentSpace,
              doors
        );
    }

    @Override
    public long getCost() {
        return 100L * (long) totalSpace;
    }
}
