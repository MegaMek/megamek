/**
 * MegaMek - Copyright (C) 2000,2001,2002,2003 Ben Mazur (bmazur@sev.org)
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

import java.util.Vector;
import megamek.common.Mounted;
import megamek.common.Mech;
import megamek.client.FiringDisplay;

/**
 * Represents intention to fire a weapon at the target.
 */
public class WeaponAttackAction
    extends AbstractAttackAction
{
    private int weaponId;
    private int ammoId = -1;
    private int aimedLocation = Mech.LOC_NONE;
    private int aimMode = FiringDisplay.AIM_MODE_NONE;
    private int otherAttackInfo = -1;	// 
    
    // equipment that affects this attack (AMS, ECM?, etc)
    // only used server-side
    private transient Vector vCounterEquipment = null;
    
    // default to attacking an entity
    public WeaponAttackAction(int entityId, int targetId, int weaponId) {
        super(entityId, targetId);
        this.weaponId = weaponId;
    }
    
    public WeaponAttackAction(int entityId, int targetType, int targetId, int weaponId) {
        super(entityId, targetType, targetId);
        this.weaponId = weaponId;
    }
    
    public int getWeaponId() {
        return weaponId;
    }
    
    public int getAmmoId() {
        return ammoId;
    }
    
    public int getAimedLocation() {
        return aimedLocation;
    }
    
    public int getAimingMode() {
        return aimMode;
    }
    
    public Vector getCounterEquipment() {
        return vCounterEquipment;
    }
    
    public void setWeaponId(int weaponId) {
        this.weaponId = weaponId;
    }
    
    public void setAmmoId(int ammoId) {
        this.ammoId = ammoId;
    }
    
    public void setAimedLocation(int aimedLocation) {
        this.aimedLocation = aimedLocation;
    }
    
    public void setAimimgMode(int aimMode) {
        this.aimMode = aimMode;
    }
    
    public void addCounterEquipment(Mounted m) {
        if (vCounterEquipment == null) {
            vCounterEquipment = new Vector();
        }
        vCounterEquipment.addElement(m);
    }

    public void setOtherAttackInfo(int newInfo) {
    	otherAttackInfo = newInfo;
    }

    public int getOtherAttackInfo() {
    	return otherAttackInfo;
    }
}
