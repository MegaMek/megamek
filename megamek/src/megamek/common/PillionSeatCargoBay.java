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
 * Represents an external or exposed support vehicle crew seat.
 */
public final class PillionSeatCargoBay extends StandardSeatCargoBay {
    private static final long serialVersionUID = 145634308684637504L;

    /**
     * The default constructor is only for serialization.
     */
    private PillionSeatCargoBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Creates pillion crew seating for support vehicles.
     *
     * @param space The number of seats
     */
    public PillionSeatCargoBay(double space) {
        super(space);
        weight = space * 0.025;
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        return "Seating (Pillion) - " + currentSpace;
    }

    @Override
    public String getType() {
        return "Pillion Seats";
    }

    @Override
    public String toString() {
        String bayType = "pillionseats";
        return this.bayString(
                bayType,
                currentSpace,
                doors
        );
    }

    @Override
    public long getCost() {
        return 10L * (long) totalSpace;
    }
}
