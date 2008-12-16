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
public class AlamoMissileWeapon extends CapitalMissileWeapon {


    /**
     * 
     */
    private static final long serialVersionUID = 3672430739887768960L;

    public AlamoMissileWeapon() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "Alamo Missile";
        this.setInternalName(BombType.getBombWeaponName(BombType.B_ALAMO));
        this.heat = 0;
        this.damage = 10;
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
        this.shortAV = 10;
        this.medAV = 10;
        this.maxRange = RANGE_MED;
        this.ammoType = AmmoType.T_ALAMO;
        this.capital = true;
    }
}
