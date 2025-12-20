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

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.common.units.LandAirMek;

/**
 * Represents a Transport Bay (TM p.239) for carrying SmallCraft or Fighters/LAMs aboard large spacecraft or other
 * units.
 */
public final class SmallCraftBay extends AbstractSmallCraftASFBay {
    @Serial
    private static final long serialVersionUID = -8275147432497460821L;

    /**
     * Create a space for the given number of small craft or fighters.
     *
     * @param space     The number of cubicles
     * @param doors     The number of bay doors
     * @param bayNumber The id number for the bay
     */
    public SmallCraftBay(double space, int doors, int bayNumber) {
        this(space, doors, bayNumber, false);
    }

    /**
     * Create a space for the given number of small craft or fighters.
     *
     * @param space     The number of cubicles
     * @param doors     The number of bay doors
     * @param bayNumber The id number for the bay
     * @param arts      Whether the bay has the advanced robotic transport system
     */
    public SmallCraftBay(double space, int doors, int bayNumber, boolean arts) {
        super(arts);
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        doorsNext = doors;
        this.currentDoors = doors;
        this.bayNumber = bayNumber;
        initializeRecoverySlots();
    }

    @Override
    public boolean canLoad(Entity unit) {
        boolean loadableAero = (unit.isFighter() || unit.isSmallCraft()) && !(unit instanceof FighterSquadron);
        boolean loadableLAM = (unit instanceof LandAirMek) && (unit.getConversionMode()
              == LandAirMek.CONV_MODE_FIGHTER);
        return (getUnused() >= 1) && (availableRecoverySlots() >= 1) && (loadableAero || loadableLAM);
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        StringBuilder sb = new StringBuilder();
        if (hasARTS()) {
            sb.append("ARTS ");
        }
        sb.append("Small Craft ").append(numDoorsString()).append(" - ")
              .append(String.format("%1$,.0f", getUnused()))
              .append(getUnused() > 1 ? " units" : " unit");
        if (showRecovery) {
            sb.append(" (").append(availableRecoverySlots()).append(" recovery open)");
        }
        return sb.toString();
    }

    @Override
    public String getTransporterType() {
        return hasARTS() ? "ARTS Small Craft" : "Small Craft";
    }

    @Override
    public double getWeight() {
        return totalSpace * (hasARTS() ? 250 : 200);
    }

    @Override
    public int getPersonnel(boolean clan) {
        return (int) totalSpace * 5;
    }

    @Override
    public String toString() {
        String bayType = (hasARTS() ? "artssmallcraftbay" : "smallcraftbay");
        return this.bayString(
              bayType,
              totalSpace,
              doors,
              bayNumber
        );
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TechBase.ALL)
              .setAdvancement(DATE_ES, DATE_ES, DATE_ES)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return hasARTS() ? Bay.artsTechAdvancement() : SmallCraftBay.techAdvancement();
    }

    @Override
    public long getCost() {
        return 20000L * (long) totalSpace;
    }
}
