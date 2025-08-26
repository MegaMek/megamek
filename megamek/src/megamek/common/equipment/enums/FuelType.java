/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.equipment.enums;

import megamek.common.enums.TechRating;

/**
 * Fuel type affects costs of refueling (if tracking fuel) and possibly vehicle operating range. See StratOps, p. 179.
 */
public enum FuelType {
    /** Currently a placeholder for "none of the others"; a better option than null */
    NONE(0, TechRating.A),
    /** Fuel cell */
    HYDROGEN(15000, TechRating.C),
    /** Standard non-aerospace ICE fuel */
    PETROCHEMICALS(1000, TechRating.A),
    /** Alternate ICE fuel */
    ALCOHOL(1500, TechRating.A),
    /** Alternate ICE fuel */
    NATURAL_GAS(1200, TechRating.A);

    /**
     * Cost in C-bills per ton. This is the cost for delivery to a forward military base. Costs can vary quite a bit in
     * other situations.
     */
    public final int cost;
    /**
     * Availability code, as with equipment
     */
    public final TechRating availability;

    FuelType(int cost, TechRating availability) {
        this.cost = cost;
        this.availability = availability;
    }
}
