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

/* Yet another marker class, until energy specific things happen */

public class LaserType extends EnergyType implements BattleArmorWeapon {
    public static final int MICRO = 0;
    public static final int SMALL = 1;
    public static final int MED   = 2;
    public static final int LARGE = 3;

    public static final int ER_MICRO = 10;
    public static final int ER_SMALL = 11;
    public static final int ER_MED   = 12;
    public static final int ER_LARGE = 13;

    protected int size;

    public LaserType (int tech, int size) {
    this.size = size;
    this.techLevel = tech;
    this.flags |= F_DIRECT_FIRE;

    if (tech == TechConstants.T_IS_LEVEL_1 ||
        tech == TechConstants.T_IS_LEVEL_2 ) {
        switch(size) {
        case SMALL:
        this.heat = 2;
        this.damage = 3;
        this.range = new RangeType(1,2,3);
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 12;
        break;
        case MED:
        this.heat = 4;
        this.damage = 6;
        this.range = new RangeType(2,4,6);
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 48;
        break;
        case LARGE:
        this.heat = 10;
        this.damage = 9;
        this.range = new RangeType(3,7,10);
        this.tonnage = 7.0f;
        this.criticals = 2;
        this.bv = 119;
        break;
        }
    } else {
        // CLAN
        switch(size) {
        case MICRO:
        this.heat = 1;
        this.damage = 3;
        this.range = new RangeType(1,2,3);
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.bv = 12;
        break;
        case SMALL:
        this.heat = 2;
        this.damage = 3;
        this.range = new RangeType(2,4,6);
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 24;
        break;
        case MED:
        this.heat = 4;
        this.damage = 7;
        this.range = new RangeType(4,8,12);
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 111;
        break;
        case LARGE:
        this.heat = 10;
        this.damage = 10;
        this.range = new RangeType(6,14,20);
        this.tonnage = 6.0f;
        this.criticals = 2;
        this.bv = 265;
        break;
            }
    }

    }


    // Micro lasers, small lasers, and small ER cannot start fire
    public int getFireTN() {
    switch(size) {
    case LaserType.MICRO:
    case LaserType.SMALL:
    case LaserType.ER_MICRO:
    case LaserType.ER_SMALL:
        return TargetRoll.IMPOSSIBLE;
    default:
        return 7;
        }
    }


    public WeaponResult setupAttack(Mounted loc, Entity en, Targetable targ) { return null; }
    public void resolveAttack( WeaponResult wr) {}
    public TargetRoll getModifiersFor(Mounted loc, Entity en, Targetable targ) { return null; }

    public void resolveBattleArmorAttack(WeaponResult wr, int num_units) {}

}
