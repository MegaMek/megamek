/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.common.bays;

/**
 * Represents a volume of space set aside for carrying units of some sort aboard large spacecraft and mobile structures
 */
public class UnitBay extends Bay {

    /**
     * The default constructor is only for serialization.
     */
    protected UnitBay() {
        super();
        minDoors = 1;
    }

    /**
     * Create a space for the given tonnage of troops. For this class, only the weight of the troops (and their
     * equipment) are considered; if you'd like to think that they are stacked like lumber, be my guest.
     *
     * @param space     The weight of troops (in tons) this space can carry.
     * @param doors     The number of bay doors
     * @param bayNumber The id number for the bay
     *
     * @deprecated No indicated uses
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public UnitBay(double space, int doors, int bayNumber) {
        super(space, doors, bayNumber);
        minDoors = 1;
    }
}
