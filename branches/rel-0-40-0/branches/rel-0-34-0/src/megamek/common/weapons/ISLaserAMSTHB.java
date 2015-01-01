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
public class ISLaserAMSTHB extends LaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -1940059603781427515L;

    /**
     * 
     */
    public ISLaserAMSTHB() {
        super();
        this.techLevel = TechConstants.T_IS_UNOFFICIAL;
        this.name = "Laser AMS (THB)";
        this.setInternalName("ISLaserAntiMissileSystemTHB");
        this.addLookupName("IS Laser Anti-Missile System (THB)");
        this.addLookupName("IS Laser AMS (THB)");
        this.heat = 3;
        this.rackSize = 2;
        this.damage = 2; // # of d6 of missiles affected
        this.ammoType = AmmoType.T_NA;
        this.tonnage = 1.5f;
        this.criticals = 2;
        this.bv = 105;
        this.flags |= F_AUTO_TARGET | F_HEATASDICE | F_AMS;
        this.setModes(new String[] { "On", "Off" });
        this.setInstantModeSwitch(false);
        this.cost = 300000;
    }
}
