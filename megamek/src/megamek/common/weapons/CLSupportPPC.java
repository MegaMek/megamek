/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLSupportPPC extends PPCWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 2062417699006705116L;

    /**
     *
     */
    public CLSupportPPC() {
        super();
        techLevel = TechConstants.T_CLAN_TW;
        name = "Support PPC";
        setInternalName(name);
        addLookupName("CLSupportPPC");
        damage = 2;
        ammoType = AmmoType.T_NA;
        shortRange = 2;
        mediumRange = 5;
        longRange = 7;
        extremeRange = 10;
        bv = 14;
        setModes(new String[] { "Field Inhibitor ON",
                "Field Inhibitor OFF" });
        cost = 14000;
    }
}
