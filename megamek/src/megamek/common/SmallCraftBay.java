/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

/**
 * Represents a Transport Bay (TM p.239) for carrying SmallCraft or Fighters/LAMs aboard large spacecraft or other units.
 */
public final class SmallCraftBay extends AbstractSmallCraftASFBay {
    private static final long serialVersionUID = -8275147432497460821L;

    /**
     * Create a space for the given number of small craft or fighters.
     *
     * @param space The number of cubicles
     * @param doors The number of bay doors
     * @param bayNumber The id number for the bay
     */
    public SmallCraftBay(double space, int doors, int bayNumber) {
        this(space, doors, bayNumber, false);
    }

    /**
     * Create a space for the given number of small craft or fighters.
     *
     * @param space The number of cubicles
     * @param doors The number of bay doors
     * @param bayNumber The id number for the bay
     * @param arts      Whether the bay has the advanced robotic transport system
     */
    public SmallCraftBay(double space, int doors, int bayNumber, boolean arts) {
        super(arts);
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        doorsNext = doors;
        this.currentdoors = doors;
        this.bayNumber = bayNumber;
        initializeRecoverySlots();
    }

    @Override
    public boolean canLoad(Entity unit) {
        boolean loadableAero = (unit.isFighter() || unit.isSmallCraft()) && !(unit instanceof FighterSquadron);
        boolean loadableLAM = (unit instanceof LandAirMech) && (unit.getConversionMode() == LandAirMech.CONV_MODE_FIGHTER);
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
    public String getType() {
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
        return (hasARTS() ? "artssmallcraftbay:" : "smallcraftbay:") + totalSpace + ":" + doors + ":" + bayNumber;
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_ES, DATE_ES, DATE_ES)
                .setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
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