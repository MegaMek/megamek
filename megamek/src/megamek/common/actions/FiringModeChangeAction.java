/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.common.actions;

/**
 * Represents intention to change a fire mode of a weapon.
 */
public class FiringModeChangeAction
    extends AbstractEntityAction
{
    private int weaponId;
    private int firingMode;
    
    public FiringModeChangeAction(int entityId, int weaponId, int firingMode) {
        super(entityId);
        this.weaponId = weaponId;
	this.firingMode = firingMode;
    }
    
    public int getWeaponId() {
        return weaponId;
    }
    
    public void setWeaponId(int weaponId) {
        this.weaponId = weaponId;
    }

    public int getFiringMode() {
	return firingMode;
    }

    public void setFiringMode(int firingMode) {
        this.firingMode = firingMode;
    }

    
}

