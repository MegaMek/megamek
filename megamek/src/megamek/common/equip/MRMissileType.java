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

/* This class defines the MRM type */

public class MRMissileType extends MissileType {

    public MRMissileType( int size, Vector valid_ammo ) {    
    super(size, valid_ammo);
    // IS ONLY

    this.techType = TechConstants.T_IS_LEVEL_2;

    switch(size) {
    case 10:
        this.tonnage = 3.0f;
        this.criticals = 2;
        this.bv = 56;
        break;
    case 20:
        this.tonnage = 7.0f;
        this.criticals = 3;
        this.bv = 112;
        break;
    case 30:
        this.tonnage = 10.0f;
        this.criticals = 5;
        this.bv = 168;
        break;
    case 40:
        this.tonnage = 12.0f;
        this.criticals = 7;
        this.bv = 224;
        break;
    }
    }

    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}
    public TargetRoll getModifiersFor(Mounted loc, Entity en, Targetable targ) { return null; }

    // MRM - 30 rolls as if it were a pair of 15 packs, and MRM-40
    // is a pair of 20's
    public int missilesHit() {
        int hits;
    if (size >= 30) {
        hits = Compute.missilesHit(size/2) +
        Compute.missilesHit(size/2);
    }
    else
        hits = Compute.missilesHit(size);

    return hits;
    }

}
