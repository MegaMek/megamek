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
import megamek.common.EquipmentType;

public abstract class LACWeapon extends ACWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -1273558621868218173L;

    public LACWeapon() {
        super();
        this.ammoType = AmmoType.T_LAC;
        this.techRating = RATING_D;
        availRating = new int[]{EquipmentType.RATING_X, EquipmentType.RATING_X,EquipmentType.RATING_F};
        introDate = 3068;
        techLevel.put(3068,techLevel.get(3071));
    }


}
