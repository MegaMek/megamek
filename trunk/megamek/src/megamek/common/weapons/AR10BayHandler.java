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
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class AR10BayHandler extends AmmoBayWeaponHandler {

    /**
     * 
     */
    
    private static final long serialVersionUID = -1618484541772117621L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public AR10BayHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }
 
    protected int getCapMisMod() {
        int mod = 0;
        for(int wId: weapon.getBayWeapons()) {
            int curr_mod = 0;
            Mounted bayW = ae.getEquipment(wId);
            //check the currently loaded ammo
            Mounted bayWAmmo = bayW.getLinked();
            AmmoType atype = (AmmoType) bayWAmmo.getType();
            if(atype == null) 
                continue;
            if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                curr_mod = 10;
            } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                curr_mod = 9;
            } else {
                curr_mod = 11;
            }
            if(curr_mod > mod) {
                mod = curr_mod;
            }
        }
        return mod;
    }
    
}
