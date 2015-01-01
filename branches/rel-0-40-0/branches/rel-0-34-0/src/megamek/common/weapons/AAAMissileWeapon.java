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

import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.TechConstants;

/**
 * @author Jay Lawson
 */
public class AAAMissileWeapon extends CapitalMissileWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2043203178614130517L;

    public AAAMissileWeapon() {
        super();
        this.techLevel = TechConstants.T_IS_ADVANCED;
        this.name = "AAA Missile";
        this.setInternalName(BombType.getBombWeaponName(BombType.B_AAA));
        this.heat = 0;
        this.damage = 20;
        this.rackSize = 1;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 24;
        this.extremeRange = 40;
        this.tonnage = 0;
        this.criticals = 0;
        this.hittable = false;
        this.bv = 0;
        this.cost = 0;
        this.shortAV = 20;
        this.medAV = 20;
        this.maxRange = RANGE_MED;
        this.ammoType = AmmoType.T_AAA_MISSILE;
        this.capital = false;
    }
}
