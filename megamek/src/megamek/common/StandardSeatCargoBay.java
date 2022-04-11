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
 * Represents a standard support vehicle crew seat.
 */
public class StandardSeatCargoBay extends Bay {
    private static final long serialVersionUID = 4161027191694822726L;

    protected double weight = 0;

    /**
     * The default constructor is only for serialization.
     */
    protected StandardSeatCargoBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Creates standard crew seating for support vehicles.
     *
     * @param space The number of seats
     */
    public StandardSeatCargoBay(double space) {
        totalSpace = currentSpace = space;
        weight = space * 0.075;
        doors = currentdoors = 0;
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
        return false;
    }

    @Override
    public String getUnusedString(boolean showrecovery) {
        return "Seating (Standard) - " + currentSpace;
    }

    @Override
    public String getType() {
        return "Standard Seats";
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
        return "standardseats:" + currentSpace + ":" + doors;
    }

    @Override
    public long getCost() {
        return 100L * (long) totalSpace;
    }
}
