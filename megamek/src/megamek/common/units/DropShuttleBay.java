/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.units;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.bays.UnitBay;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;

/**
 * Implements internal bays for dropships used by primitive jumpships. See rules IO, p. 119.
 *
 * @author Neoancient
 */
public class DropShuttleBay extends UnitBay {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -6910402023514976670L;

    // No more than one bay is allowed per armor facing
    private int facing = Entity.LOC_NONE;

    /**
     * The default constructor is only for serialization.
     */

    protected DropShuttleBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a new DropShuttle bay
     *
     * @param doors     The number of bay doors
     * @param bayNumber The bay index, unique to the Entity
     * @param facing    The armor facing of the bay
     */
    public DropShuttleBay(int doors, int bayNumber, int facing) {
        totalSpace = 2;
        currentSpace = 2;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        currentDoors = doors;
        this.facing = facing;
    }

    // Type is DropShuttle Bay
    @Override
    public String getTransporterType() {
        return "DropShuttle Bay";
    }

    @Override
    public boolean canLoad(Entity unit) {

        return unit.hasETypeFlag(Entity.ETYPE_DROPSHIP)
              && (unit.getWeight() <= 5000)
              && (currentSpace >= 1);
    }

    @Override
    public double getWeight() {
        return 11000;
    }

    @Override
    public int getFacing() {
        return facing;
    }

    /**
     * Sets the bay location
     *
     * @param facing The armor facing (location) of the bay
     */
    @Override
    public void setFacing(int facing) {
        this.facing = facing;
    }

    @Override
    public String toString() {
        String bayType = "dropshuttlebay";
        return this.bayString(
              bayType,
              totalSpace,
              doors,
              bayNumber,
              "",
              facing,
              0
        );
    }

    @Override
    public int hardpointCost() {
        return 2;
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TechBase.IS).setISAdvancement(2110, 2120, DATE_NONE, 2500)
              .setISApproximate(true, false).setTechRating(TechRating.C)
              .setPrototypeFactions(Faction.TA).setProductionFactions(Faction.TA)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    public long getCost() {
        // Set cost for 2-capacity bay
        return 150000000;
    }

}
