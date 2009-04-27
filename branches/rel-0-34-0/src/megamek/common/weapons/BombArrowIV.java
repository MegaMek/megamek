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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.TechConstants;

/**
 * @author Jay Lawson
 */
public class BombArrowIV extends AmmoWeapon {


    /**
     * 
     */
    private static final long serialVersionUID = -1321502140176775035L;

    public BombArrowIV() {
        super();
        this.techLevel = TechConstants.T_IS_ADVANCED;
        this.name = "Arrow IV (Bomb)";
        this.setInternalName(BombType.getBombWeaponName(BombType.B_ARROW));
        this.heat = 0;
        this.rackSize = 20;
        this.ammoType = AmmoType.T_ARROW_IV_BOMB;
        this.shortRange = 1; //
        this.mediumRange = 2;
        this.longRange = 9;
        this.extremeRange = 9; // No extreme range.
        this.tonnage = 0;
        this.criticals = 0;
        this.hittable = false;
        this.bv = 0;
        this.cost = 0;
    }
}
