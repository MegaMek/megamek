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

public class RotaryACType extends BallisticType {

    protected class RACEquipmentState extends UsesAmmoState {

    protected boolean jammed = false;

    public RACEquipmentState(Mounted location, RotaryACType type) {
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
    return new RACEquipmentState(location, this);
    }


    private int size;
    
    private static final String[] RAC_MODES = {"Single", "2-shot",
                           "4-shot","6-shot"};
    
    public RotaryACType( int size, Vector valid_ammo ) {
    super(valid_ammo);
    this.size = size;
    this.setModes(RAC_MODES);
    this.flags |= F_DIRECT_FIRE;
    this.techLevel = TechConstants.T_IS_LEVEL_2;

    switch(size) {
    case 2:
        this.tonnage = 8.0f;
        this.criticals = 3;
        this.bv = 118;
        break;
    case 5:
        this.tonnage = 10.0f;
        this.criticals = 6;
        this.bv = 247;
        break;
    }
    }
    
    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr){}
    public TargetRoll getModifiersFor(Mounted loc, Entity en, Targetable targ) { return null; }

}
