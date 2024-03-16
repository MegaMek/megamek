/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.lrms;

/**
 * @author Sebastian Brocks
 */
public class CLStreakLRM5 extends StreakLRMWeapon {
    private static final long serialVersionUID = 5240577239366457930L;

    public CLStreakLRM5() {
        super();
        name = "Streak LRM 5";
        setInternalName("CLStreakLRM5");
        addLookupName("Clan Streak LRM-5");
        addLookupName("Clan Streak LRM 5");
        heat = 2;
        rackSize = 5;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 2.0;
        criticals = 1;
        bv = 86;
        cost = 75000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        maxRange = RANGE_LONG;
        rulesRefs = "327, TO";
        // Tech Advancement moved to StreakLRMWeapon.java
    }
}
