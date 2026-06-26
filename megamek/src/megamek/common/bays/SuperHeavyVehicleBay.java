/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.units.Entity;
import megamek.common.units.QuadVee;
import megamek.common.units.Tank;

/**
 * Represents a Transport Bay (TM p.239) for carrying superheavy vehicles (not heavier than 200t) aboard large
 * spacecraft or other units.
 */
public final class SuperHeavyVehicleBay extends UnitBay {
    @Serial
    private static final long serialVersionUID = 3490408642054662664L;

    /**
     * Create a space for the given number of vehicles.
     *
     * @param space     The number of vehicles that can be carried
     * @param bayNumber The id number for the bay
     */
    public SuperHeavyVehicleBay(double space, int doors, int bayNumber) {
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        currentDoors = doors;
    }

    @Override
    public boolean canLoad(Entity unit) {
        boolean loadableQuadVee = (unit instanceof QuadVee) && (unit.getConversionMode() == QuadVee.CONV_MODE_VEHICLE);
        return (getUnused() >= 1) && (currentDoors >= loadedThisTurn)
              && (unit.getWeight() <= 200) && ((unit instanceof Tank) || loadableQuadVee);
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        return "Superheavy Vehicle Bay " + numDoorsString() + " - "
              + String.format("%1$,.0f", getUnused())
              + (getUnused() > 1 ? " units" : " unit");
    }

    @Override
    public String getTransporterType() {
        return "Superheavy Vehicle";
    }

    @Override
    public double getWeight() {
        return totalSpace * 200;
    }

    @Override
    public int getPersonnel(boolean clan) {
        return (int) totalSpace * 15;
    }

    @Override
    public String toString() {
        String bayType = "superheavyvehiclebay";
        return this.bayString(
              bayType,
              totalSpace,
              doors,
              bayNumber
        );
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TechBase.ALL)
              .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
              .setTechRating(TechRating.A)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return SuperHeavyVehicleBay.techAdvancement();
    }

    @Override
    public long getCost() {
        return 20000L * (long) totalSpace;
    }
}
