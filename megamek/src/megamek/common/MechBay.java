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
 * Represents a Transport Bay (TM p.239) for carrying Meks or LAMs aboard large spacecraft other units.
 */
public final class MechBay extends Bay {
    private static final long serialVersionUID = 927162989742234173L;

    /**
     * Create a Mek Bay for a given number of mek units.
     *
     * @param space The number of Meks this bay can hold
     * @param bayNumber The number of this bay on its unit
     */
    public MechBay(double space, int doors, int bayNumber) {
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        currentdoors = doors;
    }

    @Override
    public boolean canLoad(Entity unit) {
        boolean loadableQuadVee = (unit instanceof QuadVee) && (unit.getConversionMode() == QuadVee.CONV_MODE_MECH);
        boolean loadableLAM = (unit instanceof LandAirMech) && (unit.getConversionMode() != LandAirMech.CONV_MODE_FIGHTER);
        boolean loadableOtherMek = (unit instanceof Mech) && !(unit instanceof QuadVee) && !(unit instanceof LandAirMech);
        return (getUnused() >= 1) && (doors > loadedThisTurn) && (loadableLAM || loadableQuadVee || loadableOtherMek);
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        return "Mech " + numDoorsString() + " - "
                + String.format("%1$,.0f", getUnused())
                + (getUnused() > 1 ? " units" : " unit");
    }

    @Override
    public String getType() {
        return "Mech";
    }

    @Override
    public double getWeight() {
        return totalSpace * 150;
    }

    @Override
    public int getPersonnel(boolean clan) {
        return (int) totalSpace * 2;
    }

    @Override
    public String toString() {
        return "mechbay:" + totalSpace + ":" + doors;
    }
    
    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(2445, 2470, 2500)
                .setApproximate(true, false, false).setTechRating(RATING_C)
                .setAvailability(RATING_D, RATING_C, RATING_C, RATING_C)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
    
    @Override
    public TechAdvancement getTechAdvancement() {
        return MechBay.techAdvancement();
    }

    @Override
    public long getCost() {
        return 20000L * (long) totalSpace;
    }
}