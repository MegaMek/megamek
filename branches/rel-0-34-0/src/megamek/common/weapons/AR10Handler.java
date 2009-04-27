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
package megamek.common.weapons;


import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class AR10Handler extends AmmoWeaponHandler {

    /**
     * 
     */
    private static final long serialVersionUID = -2536312899803153911L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public AR10Handler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }
    
    /**
     * Calculate the attack value based on range
     * 
     * @return an <code>int</code> representing the attack value at that range.
     */
    protected int calcAttackValue() {
        int av = 0;       
        AmmoType atype = (AmmoType) ammo.getType();
        if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
            av = 4;
        } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
            av = 3;
        } else {
            av =2;
        }
        return av;
    }
    
    protected int getCapMisMod() {
        int mod = 0;
        AmmoType atype = (AmmoType) ammo.getType();
        if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
            mod = 10;
        } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
            mod = 9;
        } else {
            mod = 11;
        }
        return mod;
    }

}
