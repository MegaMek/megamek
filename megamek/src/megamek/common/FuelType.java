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

/**
 * Fuel type affects costs of refueling (if tracking fuel) and possibly vehicle operating
 * range. See StratOps, p. 179.
 */
public enum FuelType {
    NONE (0, ITechnology.RATING_A),
    HYRDOGEN (15000, ITechnology.RATING_C),
    PETROCHEMICALS (1000, ITechnology.RATING_A),
    ALCOHOL (1500, ITechnology.RATING_A),
    NATURAL_GAS (1200, ITechnology.RATING_A);

    /**
     * Cost in C-bills per ton. This is the cost for delivery to a forward military
     * base. Costs can vary quite a bit in other situations.
     */
    public final int cost;
    /**
     * Availabilty code, as with equipment
     */
    public final int availability;

    FuelType(int cost, int availability) {
        this.cost = cost;
        this.availability = availability;
    }
}
