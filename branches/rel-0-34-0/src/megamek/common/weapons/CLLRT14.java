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

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLLRT14 extends LRTWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 7642378254853022847L;

    /**
     * 
     */
    public CLLRT14() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "LRT 14";
        this.setInternalName("CLLRTorpedo14");
        this.heat = 0;
        this.rackSize = 14;
        this.minimumRange = WEAPON_NA;
        this.waterShortRange = 7;
        this.waterMediumRange = 14;
        this.waterLongRange = 21;
        this.waterExtremeRange = 28;
        this.tonnage = 2.8f;
        this.criticals = 0;
        this.bv = 163;
    }
}
