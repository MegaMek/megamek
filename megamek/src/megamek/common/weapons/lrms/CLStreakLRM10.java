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
public class CLStreakLRM10 extends StreakLRMWeapon {
    private static final long serialVersionUID = 7179570524181470428L;

    public CLStreakLRM10() {
        super();
        name = "Streak LRM 10";
        setInternalName("CLStreakLRM10");
        addLookupName("Clan Streak LRM-10");
        addLookupName("Clan Streak LRM 10");
        heat = 4;
        rackSize = 10;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 5.0;
        criticals = 2;
        bv = 173;
        cost = 225000;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_LONG;
        rulesRefs = "327, TO";
        // Tech Advancement moved to StreakLRMWeapon.java
    }
}
