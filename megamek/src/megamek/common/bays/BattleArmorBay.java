/*
 * Copyright (c) 2003-2004 - Ben Mazur (bmazur@sev.org).
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

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.interfaces.ITechnology;
import megamek.common.units.Entity;
import megamek.common.units.InfantryTransporter;

/**
 * Represents a volume of space set aside for carrying Battle Armor squads aboard large spacecraft and mobile
 * structures
 */
public final class BattleArmorBay extends Bay implements InfantryTransporter {
    @Serial
    private static final long serialVersionUID = 7091227399812361916L;

    private boolean isClan = false;
    private boolean isComStar = false;

    /**
     * The default constructor is only for serialization.
     */
    private BattleArmorBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a space for the given tonnage of troops. For this class, only the weight of the troops (and their
     * equipment) are considered; if you'd like to think that they are stacked like lumber, be my guest.
     *
     * @param space The weight of troops (in tons) this space can carry.
     */
    public BattleArmorBay(double space, int doors, int bayNumber, boolean isClan, boolean isComStar) {
        totalSpace = space;
        currentSpace = space;
        this.minDoors = 0;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        this.isClan = isClan;
        this.isComStar = isComStar;
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
        // Assume that we cannot carry the unit.
        // Only Battle Armor squads
        boolean result = unit instanceof BattleArmor;

        // We must have enough space for the new troops.
        // POSSIBLE BUG: we may have to take the Math.ceil() of the weight.
        if (getUnused() < 1) {
            result = false;
        }

        // Return our result.
        return result;
    }

    @Override
    public boolean canUnloadUnits() {
        // Infantry is only restricted by adjacency requirements (TW pp. 223 - 225)
        return super.canUnloadUnits() || troops.stream()
              .map(unit -> game.getEntity(unit))
              .anyMatch(e -> (e != null && e.isBattleArmor()));
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        return "Battle Armor Bay " + numDoorsString() + " - "
              + String.format("%1$,.0f", getUnused())
              + (getUnused() > 1 ? isClan ? " Points"
              : isComStar ? " Level I" : " Squads"
              : isClan ? " Point" : isComStar ? " Level I" : " Squad");
    }

    @Override
    public String getTransporterType() {
        return "Battle Armor";
    }

    @Override
    public double getWeight() {
        return totalSpace * 2 * (isClan ? 5 : (isComStar ? 6 : 4));
    }

    @Override
    public int getPersonnel(boolean clan) {
        return (int) totalSpace * 6;
    }

    @Override
    public String toString() {
        // See BLKFile.java:BLKFile constants
        int bitmap = 0;
        bitmap |= (isComStar ? 1 : 0);
        bitmap |= (isClan ? 1 << 1 : 0);
        String bayType = "battlearmorbay";
        return this.bayString(
              bayType,
              totalSpace,
              doors,
              bayNumber,
              "",
              Entity.LOC_NONE,
              bitmap
        );
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TechBase.ALL)
              .setClanAdvancement(2867, 2868, 2870, ITechnology.DATE_NONE, ITechnology.DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setISAdvancement(ITechnology.DATE_NONE,
                    ITechnology.DATE_NONE,
                    3050,
                    ITechnology.DATE_NONE,
                    ITechnology.DATE_NONE)
              .setPrototypeFactions(Faction.CWF).setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X,
                    AvailabilityValue.X,
                    AvailabilityValue.C,
                    AvailabilityValue.B)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return BattleArmorBay.techAdvancement();
    }

    @Override
    public boolean isClan() {
        return isClan;
    }

    public boolean isComStar() {
        return isComStar;
    }

    @Override
    public long getCost() {
        // Based on the weight of the equipment (not capacity), rounded up to the whole ton
        return 15000L * (long) Math.ceil(getWeight());
    }
}
