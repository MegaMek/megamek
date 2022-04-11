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
public class CLStreakLRM20 extends StreakLRMWeapon {
    private static final long serialVersionUID = -7125174806713066191L;

    public CLStreakLRM20() {
        super();
        name = "Streak LRM 20";
        setInternalName("CLStreakLRM20");
        addLookupName("Clan Streak LRM-20");
        addLookupName("Clan Streak LRM 20");
        heat = 6;
        rackSize = 20;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 10.0;
        criticals = 5;
        bv = 345;
        cost = 600000;
        shortAV = 20;
        medAV = 20;
        longAV = 20;
        maxRange = RANGE_LONG;
        rulesRefs = "327, TO";
        // Tech Advancement moved to StreakLRMWeapon.java
    }
}
