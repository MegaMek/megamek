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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISBAMagshotGaussRifle extends Weapon {
    /**
     * 
     */
    private static final long serialVersionUID = 870812653880382979L;

    /**
     * 
     */
    public ISBAMagshotGaussRifle() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Magshot";
        this.setInternalName("ISBAMagshotGR");
        this.damage = 2;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 0.5f;
        this.criticals = 2;
        this.bv = 15;
        this.cost = 10500;
        this.flags |= F_BA_WEAPON;
    }
}
