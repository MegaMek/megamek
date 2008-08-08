/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons;

/**
 * @author Andrew Hunter
 */
public abstract class GaussWeapon extends AmmoWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8640523093316267351L;

    public GaussWeapon() {
        super();
        this.flags |= F_DIRECT_FIRE | F_NO_FIRES | F_BALLISTIC;
        this.explosive = true;
        
        this.atClass = CLASS_AC;
    }

}
