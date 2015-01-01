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
public class ASMissileWeapon extends CapitalMissileWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 8263429182520693147L;

    public ASMissileWeapon() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "AS Missile";
        this.setInternalName(BombType.getBombWeaponName(BombType.B_AS));
        this.heat = 0;
        this.damage = 30;
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
        this.shortAV = 30;
        this.medAV = 30;
        this.longAV = 30;
        this.flags |= F_ANTI_SHIP;
        this.maxRange = RANGE_LONG;
        this.ammoType = AmmoType.T_AS_MISSILE;
        this.capital = false;
    }
}
