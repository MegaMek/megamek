/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.weapons;

/**
 * @author Sebastian Brocks
 */
public class CLStreakSRM6Prototype extends CLPrototypeStreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2234544642223178737L;

    /**
     *
     */
    public CLStreakSRM6Prototype() {
        super();
        name = "Streak SRM 6 (CP)";
        setInternalName("CLStreakSRM6Prototype");
        heat = 4;
        rackSize = 6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 4.5f;
        criticals = 2;
        bv = 59;
        cost = 80000;
        shortAV = 8;
        maxRange = RANGE_SHORT;
    }
}
