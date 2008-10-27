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
/*
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLAMS extends AmmoWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 7447941274169853546L;

    /**
     * 
     */
    public CLAMS() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "AMS";
        this.setInternalName("CLAntiMissileSystem");
        this.addLookupName("Clan Anti-Missile Sys");
        this.addLookupName("Clan AMS");
        this.heat = 1;
        this.rackSize = 2;
        this.damage = 2; // # of d6 of missiles affected
        this.ammoType = AmmoType.T_AMS;
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.bv = 32;
        this.flags |= F_AUTO_TARGET | F_AMS | F_BALLISTIC;
        this.setModes(new String[] { "On", "Off" });
        this.setInstantModeSwitch(false);
        this.cost = 100000;
        
        this.atClass = CLASS_POINT_DEFENSE;
    }
}
