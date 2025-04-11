/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
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
 */
package megamek.common.bays;

import java.io.Serial;

import megamek.common.Entity;
import megamek.common.ProtoMek;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;

/**
 * Represents a volume of space set aside for carrying ProtoMeks aboard large spacecraft and mobile structures
 */
public final class ProtoMekBay extends UnitBay {
    @Serial
    private static final long serialVersionUID = 927162989742234173L;

    /**
     * Create a space for the given tonnage of troops. For this class, only the weight of the troops (and their
     * equipment) are considered; if you'd like to think that they are stacked like lumber, be my guest.
     *
     * @param space     The weight of troops (in tons) this space can carry.
     * @param bayNumber Number of the Bay we are in
     */
    public ProtoMekBay(double space, int doors, int bayNumber) {
        totalSpace = space;
        currentSpace = space;
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
        if (getUnused() < 1) {
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
        return "ProtoMek " +
                     numDoorsString() +
                     " - " +
                     String.format("%1$,.0f", getUnused()) +
                     (getUnused() > 1 ? " units" : " unit");
    }

    @Override
    public String getType() {
        return "ProtoMek";
    }

    @Override
    public double getWeight() {
        return totalSpace * 10;
    }

    @Override
    public int getPersonnel(boolean clan) {
        return (int) totalSpace * 6;
    }

    @Override
    public String toString() {
        String bayType = "protoMekBay";
        return this.bayString(bayType, totalSpace, doors, bayNumber);
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_CLAN).setClanAdvancement(3060, 3066, 3070)
                     .setClanApproximate(true, false, false)
                     .setTechRating(RATING_C)
                     .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                     .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return ProtoMekBay.techAdvancement();
    }

    @Override
    public long getCost() {
        // Cost is per five cubicles
        return 10000L * (long) Math.ceil(totalSpace / 5);
    }

    @Override
    public BayType getBayType() {
        return BayType.PROTOMEK;
    }

    @Override
    public BayData getBayData() {
        return BayData.PROTOMEK;
    }

}
