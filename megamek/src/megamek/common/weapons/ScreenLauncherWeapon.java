/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.TechConstants;
import megamek.common.AmmoType;

/**
 * @author Jay Lawson
 */
public class ScreenLauncherWeapon extends AmmoWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public ScreenLauncherWeapon() {

        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "Screen Launcher";
        this.setInternalName(this.name);
        this.addLookupName("ScreenLauncher");
        this.heat = 10;
        this.damage = 15;
        this.ammoType = AmmoType.T_SCREEN_LAUNCHER;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 40.0f;
        this.bv = 160;
        this.cost = 250000;
            
        this.maxRange = RANGE_SHORT;
        this.capital = true;
        this.atClass = CLASS_SCREEN;
    }
}