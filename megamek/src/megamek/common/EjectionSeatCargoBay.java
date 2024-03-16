/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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
    private EjectionSeatCargoBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

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
    public String getUnusedString(boolean showRecovery) {
        return "Seating (Ejection) - " + currentSpace;
    }

    @Override
    public String getType() {
        return "Ejection Seats";
    }

    @Override
    public String toString() {
        String bayType = "ejectionseats";
        return this.bayString(
                bayType,
                currentSpace,
                doors
        );
    }

    @Override
    public long getCost() {
        return 25000 * (long) totalSpace;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return EjectionSeatCargoBay.techAdvancement();
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setTechRating(RATING_B)
                .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE);
    }
}
