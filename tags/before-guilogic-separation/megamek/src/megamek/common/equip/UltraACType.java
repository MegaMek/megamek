/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common.equip;

import megamek.common.*;
import java.util.Vector;

/* This class defines all of the ballistic weapon subtypes. */

public class UltraACType extends BallisticType {

    protected class UACEquipmentState extends UsesAmmoState {

        protected boolean jammed = false;

        public UACEquipmentState(Mounted location, UltraACType type) {
            super(location, type);
        }
    
        public boolean isJammed() {
            return jammed;
        }
        
        public void setJammed(boolean jammed) {
            this.jammed = jammed;
        }
       
    }
    
    public EquipmentState getNewState(Mounted location) {
    return new UACEquipmentState(location, this);
    }

    private static final String[] UAC_MODES = {"Single", "Ultra"};

    private int size;

    public UltraACType( int tech, int size, Vector valid_ammo ) {
    super(valid_ammo);
    this.size = size;
    this.setModes(UAC_MODES);
    this.flags |= F_DIRECT_FIRE;
    this.techType = tech;

    if (tech == TechConstants.T_IS_LEVEL_2) {
        switch(size) {
        case 2:
        this.tonnage = 7.0f;
        this.criticals = 3;
        this.bv = 56;
        break;
        case 5:
        this.tonnage = 9.0f;
        this.criticals = 5;
        this.bv = 113;
        break;
        case 10:
        this.tonnage = 13.0f;
        this.criticals = 7;
        this.bv = 253;
        break;
        case 20:
        this.tonnage = 15.0f;
        this.criticals = 10;
        this.bv = 282;
        break;
        }
    } else { // Clan TECH
        switch(size) {
        case 2:
        this.tonnage = 5.0f;
        this.criticals = 2;
        this.bv = 62;
        break;
        case 5:
        this.tonnage = 7.0f;
        this.criticals = 3;
        this.bv = 123;
        break;
        case 10:
        this.tonnage = 10.0f;
        this.criticals = 4;
        this.bv = 211;
        break;
        case 20:
        this.tonnage = 12.0f;
        this.criticals = 8;
        this.bv = 337;
        break;
        }
    }
    

    }
    
    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}
}
