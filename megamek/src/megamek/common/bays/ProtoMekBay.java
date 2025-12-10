/*
  Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
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

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.units.Entity;
import megamek.common.units.ProtoMek;

/**
 * Represents a volume of space set aside for carrying ProtoMek points aboard large spacecraft and mobile structures.
 */
public final class ProtoMekBay extends UnitBay {
    @Serial
    private static final long serialVersionUID = 927162989742234173L;

    /**
     * The default constructor is only for serialization.
     */
    private ProtoMekBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a space for the given tonnage of troops. For this class, only the weight of the troops (and their
     * equipment) are considered; if you'd like to think that they are stacked like lumber, be my guest.
     *
     * @param space The number of ProtoMeks that can fit in this bay.
     */
    public ProtoMekBay(double space, int doors, int bayNumber) {
        totalSpace = Math.ceil(space / 5);
        currentSpace = totalSpace;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        currentDoors = doors;
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
        // Assume that we cannot carry the unit, unless it is a ProtoMek
        boolean result = unit instanceof ProtoMek;

        // We must have enough space for the new troops.
        // TODO : POSSIBLE BUG : we may have to take the Math.ceil() of the weight.
        if (getUnused() < 0.2) {
            result = false;
        }

        // is the door functional
        if (doors <= loadedThisTurn) {
            result = false;
        }

        // Return our result.
        return result;
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        return "ProtoMek " + numDoorsString() + " - "
              + String.format("%1$,.0f", getUnusedSlots())
              + (getUnusedSlots() > 1 ? " units" : " unit");
    }

    @Override
    public String getTransporterType() {
        return "ProtoMek";
    }

    @Override
    public double getWeight() {
        return totalSpace * 50;
    }

    @Override
    public int getPersonnel(boolean clan) {
        return (int) Math.ceil(totalSpace) * 6;
    }

    @Override
    public String toString() {
        String bayType = "ProtoMekBay";
        return this.bayString(
              bayType,
              totalSpace,
              doors,
              bayNumber
        );
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TechBase.CLAN).setClanAdvancement(3060, 3066, 3070)
              .setClanApproximate(true, false, false).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return ProtoMekBay.techAdvancement();
    }

    @Override
    public long getCost() {
        // Cost is per five cubicles
        return 10000L * (long) Math.ceil(totalSpace);
    }

    @Override
    public double spaceForUnit(Entity unit) {
        return 0.2;
    }

    @Override
    public double getUnusedSlots() {
        return getUnused() * 5;
    }

    @Override
    public String getNameForRecordSheets() {
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        return "ProtoMech";
    }
}
