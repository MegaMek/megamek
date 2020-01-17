/*
 * MegaMek - Copyright (C) 2019 - The MegaMek Team
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
 * Support vehicle ejection seats.
 */
public final class EjectionSeatCargoBay extends StandardSeatCargoBay {

    private static final long serialVersionUID = 8916801835963112628L;

    /**
     * The default constructor is only for serialization.
     */
    protected EjectionSeatCargoBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    // Public constructors and methods.

    /**
     * Creates pillion crew seating for support vehicles.
     *
     * @param space The number of seats
     */
    public EjectionSeatCargoBay(double space) {
        super(space);
        weight = space * 0.1;
    }

    @Override
    public String getUnusedString(boolean showrecovery) {
        return "Seating (Ejection) - " + currentSpace;
    }

    @Override
    public String getType() {
        return "Ejection Seats";
    }

    @Override
    public String toString() {
        return "ejectionseats:" + currentSpace + ":" + doors;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return EjectionSeatCargoBay.techAdvancement();
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE);
    }
}
