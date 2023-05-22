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
 * Represents a Transport Bay (TM p.239) for carrying Fighters/LAMs aboard large spacecraft or other units.
 */
public final class ASFBay extends AbstractSmallCraftASFBay {
    private static final long serialVersionUID = -4110012474950158433L;

    /**
     * Create a space for the given number of fighters.
     *
     * @param space      The number of cubicles
     * @param doors      The number of bay doors
     * @param bayNumber  The id number for the bay
     */
    public ASFBay(double space, int doors, int bayNumber) {
        this(space, doors, bayNumber, false);
    }

    /**
     * Create a space for the given number of fighters.
     *
     * @param space The number of cubicles
     * @param doors The number of bay doors
     * @param bayNumber The id number for the bay
     * @param arts Whether the bay has the advanced robotic transport system
     */
    public ASFBay(double space, int doors, int bayNumber, boolean arts) {
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
        boolean loadableFighter = unit.isFighter() && !(unit instanceof FighterSquadron);
        boolean loadableLAM = (unit instanceof LandAirMech) && (unit.getConversionMode() == LandAirMech.CONV_MODE_FIGHTER);
        return (getUnused() >= 1) && (availableRecoverySlots() >= 1) && (loadableFighter || loadableLAM);
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        StringBuilder sb = new StringBuilder();
        if (hasARTS()) {
            sb.append("ARTS ");
        }
        sb.append("Aerospace Fighter ");
        if (showRecovery) {
            sb.append(numDoorsString()).append(" - ")
                .append(String.format("%1$,.0f", getUnused()))
                .append(" units (").append(availableRecoverySlots()).append(" recovery open)");
        } else {
            sb.append(String.format(" Bay %s - %2$,.0f", numDoorsString(), getUnused()))
                    .append(getUnused() > 1 ? " units" : " unit");
        }
        return sb.toString();
    }

    @Override
    public String getType() {
        return hasARTS() ? "ARTS Fighter" : "Fighter";
    }

    @Override
    public double getWeight() {
        return totalSpace * (hasARTS() ? 187.5 : 150);
    }

    @Override
    public int getPersonnel(boolean clan) {
        return hasARTS() ? 0 : (int) totalSpace * 2;
    }

    @Override
    public String toString() {
        return (hasARTS() ? "artsasfbay:" : "asfbay:") + totalSpace + ":" + doors + ":" + bayNumber;
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_ES, DATE_ES, DATE_ES)
                .setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
    
    @Override
    public TechAdvancement getTechAdvancement() {
        return hasARTS() ? Bay.artsTechAdvancement() : ASFBay.techAdvancement();
    }

    @Override
    public long getCost() {
        return 20000L * (long) totalSpace + (hasARTS() ? 1000000L : 0);
    }
}