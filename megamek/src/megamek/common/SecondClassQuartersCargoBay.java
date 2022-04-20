/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
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
 * Represents a volume of space set aside for carrying second class passengers
 */
public final class SecondClassQuartersCargoBay extends Bay {
    private static final long serialVersionUID = 4161027191694822726L;

    private double weight = 0;

    /**
     * The default constructor is only for serialization.
     */
    private SecondClassQuartersCargoBay() {
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
    public SecondClassQuartersCargoBay(double weight, int doors) {
        totalSpace = (int) weight / 7;
        this.weight = weight;
        currentSpace = (int) weight / 7;
        this.doors = doors;
        currentdoors = doors;
    }

    /**
     * Create space for certain number of crew/passengers
     *
     * @param space The number of crew or passengers to accomodate
     */
    public SecondClassQuartersCargoBay(int space) {
        this(space * 7, 0);
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
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
        StringBuffer returnString = new StringBuffer("2nd Class Quarters ("
                + getCurrentDoors() + " doors) - ");
        returnString.append((int) currentSpace);
        return returnString.toString();
    }

    @Override
    public String getType() {
        return "2nd Class Quarters";
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
        return "2ndclassquarters:" + weight + ":" + doors;
    }

    @Override
    public long getCost() {
        return 15000L * (long) totalSpace;
    }
}
