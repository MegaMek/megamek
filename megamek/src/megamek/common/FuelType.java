/*
 * MegaMek
 * Copyright (C) 2019 The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package megamek.common;

import megamek.common.ITechnology.TechRating;

/**
 * Fuel type affects costs of refueling (if tracking fuel) and possibly vehicle operating
 * range. See StratOps, p. 179.
 */
public enum FuelType {
    /** Currently a place holder for "none of the others"; a better option than null */
    NONE (0, ITechnology.TechRating.A),
    /** Fuel cell */
    HYRDOGEN (15000, ITechnology.TechRating.C),
    /** Standard non-aerospace ICE fuel */
    PETROCHEMICALS (1000, ITechnology.TechRating.A),
    /** Alternate ICE fuel */
    ALCOHOL (1500, ITechnology.TechRating.A),
    /** Alternal ICE fuel */
    NATURAL_GAS (1200, ITechnology.TechRating.A);

    /**
     * Cost in C-bills per ton. This is the cost for delivery to a forward military
     * base. Costs can vary quite a bit in other situations.
     */
    public final int cost;
    /**
     * Availabilty code, as with equipment
     */
    public final TechRating availability;

    FuelType(int cost, TechRating availability) {
        this.cost = cost;
        this.availability = availability;
    }
}
