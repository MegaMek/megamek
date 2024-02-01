/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022-2023 - The MegaMek Team. All Rights Reserved.
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
 * Represents a Transport Bay (TM p.239) for carrying superheavy vehicles (not heavier than 200t)
 * aboard large spacecraft or other units.
 */
public final class SuperHeavyVehicleBay extends Bay {
    private static final long serialVersionUID = 3490408642054662664L;

    /**
     * Create a space for the given number of vehicles.
     *
     * @param space The number of vehicles that can be carried
     * @param bayNumber The id number for the bay
     */
    public SuperHeavyVehicleBay(double space, int doors, int bayNumber) {
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        currentdoors = doors;
    }

    @Override
    public boolean canLoad(Entity unit) {
        boolean loadableQuadVee = (unit instanceof QuadVee) && (unit.getConversionMode() == QuadVee.CONV_MODE_VEHICLE);
        return (getUnused() >= 1) && (currentdoors >= loadedThisTurn)
                && (unit.getWeight() <= 200) && ((unit instanceof Tank) || loadableQuadVee);
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        return "Superheavy Vehicle Bay " + numDoorsString() + " - "
                + String.format("%1$,.0f", getUnused())
                + (getUnused() > 1 ? " units" : " unit");
    }

    @Override
    public String getType() {
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
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
                .setTechRating(RATING_A)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
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
