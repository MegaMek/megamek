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
 * Represents a volume of space set aside for carrying livestock
 */

public final class LivestockCargoBay extends Bay {
    private static final long serialVersionUID = 4161027191694822726L;

    private double weight = 0;

    /**
     * The default constructor is only for serialization.
     */
    private LivestockCargoBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a space for the given tonnage of troops. For this class, only the
     * weight of the troops (and their equipment) are considered; if you'd like
     * to think that they are stacked like lumber, be my guest.
     *
     * @param space The weight of troops (in tons) this space can carry.
     * @param bayNumber
     */
    public LivestockCargoBay(double space, int doors, int bayNumber) {
        totalSpace = space;
        weight = space / 0.83;
        currentSpace = space;
        this.doors = doors;
        this.bayNumber = bayNumber;
        currentdoors = doors;
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
    public String getUnusedString(boolean showrecovery) {
        StringBuffer returnString = new StringBuffer("Livestock Cargo Space "
                + numDoorsString() + " - ");

        if (getUnused() != Math.round(getUnused())) {
            returnString.append(String.format("%1$,.3f", getUnused()));
        } else {
            returnString.append(String.format("%1$,.0f", getUnused()));
        }

        returnString.append(" tons");
        return returnString.toString();
    }

    @Override
    public String getType() {
        return "Livestock Cargo";
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return "livestockcargobay:" + totalSpace + ":" + doors + ":"+ bayNumber;
    }

    
    @Override
    public boolean isCargo() {
        return true;
    }

    @Override
    public long getCost() {
        // Based on the weight of the equipment (not capacity), rounded up to the whole ton
        return 2500L * (long) Math.ceil(getWeight());
    }
}
