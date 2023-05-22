/*
 * Copyright (c) 2003-2004 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

/**
 * Represents a (cramped) volume of space set aside for carrying a mobile structure or spacecraft's
 * crew
 */
public final class SteerageQuartersCargoBay extends Bay {
    private static final long serialVersionUID = 4161027191694822726L;

    private double weight = 0;

    /**
     * The default constructor is only for serialization.
     */
    private SteerageQuartersCargoBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a space for the given tonnage of troops. For this class, only the
     * weight of the troops (and their equipment) are considered; if you'd like
     * to think that they are stacked like lumber, be my guest.
     *
     * @param weight The weight of troops (in tons) this space can carry.
     */
    public SteerageQuartersCargoBay(double weight, int doors) {
        totalSpace = ((int) weight) / 5;
        this.weight = weight;
        currentSpace = ((int) weight) / 5;
        this.doors = doors;
        currentdoors = doors;
    }

    /**
     * Create space for certain number of crew/passengers
     *
     * @param space The number of crew or passengers to accommodate
     */
    public SteerageQuartersCargoBay(int space) {
        this(space * 5, 0);
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be of the appropriate
     * type or there may be no room for the unit.
     *
     * @param unit the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code> otherwise.
     */
    @Override
    public boolean canLoad(Entity unit) {
        // Assume that we cannot carry the unit.
        return false;
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        return "Steerage Quarters (" + getCurrentDoors() + " doors) - " + (int) currentSpace;
    }

    @Override
    public String getType() {
        return "Steerage Quarters";
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public boolean isQuarters() {
        return true;
    }

    @Override
    public String toString() {
        return "steeragequarters:" + weight + ":" + doors;
    }

    @Override
    public long getCost() {
        return 5000L * (long) totalSpace;
    }
}
