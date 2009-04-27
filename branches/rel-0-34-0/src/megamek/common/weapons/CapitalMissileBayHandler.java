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

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class CapitalMissileBayHandler extends AmmoBayWeaponHandler {

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
    public CapitalMissileBayHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
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
            curr_mod = getCritMod(atype);
            if(curr_mod > mod) {
                mod = curr_mod;
            }
        }
        return mod;
    }
    
    /*
     * get the cap mis mod given a single ammo type
     */
    protected int getCritMod(AmmoType atype) {
        if(atype == null || atype.getAmmoType() == AmmoType.T_PIRANHA) 
            return 0;
        if (atype.getAmmoType() == AmmoType.T_WHITE_SHARK 
                || atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
            return 9;
        } else if (atype.getAmmoType() == AmmoType.T_KILLER_WHALE 
                || atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE) 
                || atype.getAmmoType() == AmmoType.T_MANTA_RAY) {
            return 10;
        } else if (atype.getAmmoType() == AmmoType.T_KRAKEN_T) {
            return 8;
        } else if (atype.getAmmoType() == AmmoType.T_STINGRAY) {
            return 12;
        } else {
            return 11;
        }
    }
    
    /**
     * Insert any additionaly attacks that should occur before this attack
     */
    protected void insertAttacks(IGame.Phase phase, Vector<Report> vPhaseReport) {
        for(int wId: insertedAttacks) {
            Mounted bayW = ae.getEquipment(wId);
            WeaponAttackAction newWaa = new WeaponAttackAction(ae.getId(),waa.getTargetId(), wId);
            Weapon w = (Weapon) bayW.getType();
            //increase ammo by one, we'll use one that we shouldn't use
            // in the next line
            bayW.getLinked().setShotsLeft(bayW.getLinked().getShotsLeft()+1);
            (w.fire(newWaa, game, server)).handle(phase, vPhaseReport);
        }
    }
}
