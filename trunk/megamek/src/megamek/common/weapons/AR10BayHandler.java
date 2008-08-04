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
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.Report;
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
        if(atype == null) 
            return 0;
        if (atype.getAmmoType() == AmmoType.T_WHITE_SHARK) {
            return 9;
        } else if (atype.getAmmoType() == AmmoType.T_KILLER_WHALE) {
            return 10;
        } else if (atype.getAmmoType() == AmmoType.T_KRAKEN_T) {
            return 8;
        } else {
            return 11;
        }
    }
    
    /**
     * special resolution, like minefields and arty
     * 
     * @param vPhaseReport - a <code>Vector</code> containing the phase report
     * @param entityTarget - the <code>Entity</code> targeted, or
     *            <code>null</code>, if no Entity targeted
     * @param bMissed - a <code>boolean</code> value indicating wether the
     *            attack missed or hit
     * @return true when done with processing, false when not
     */
    protected boolean specialResolution(Vector<Report> vPhaseReport,
            Entity entityTarget, boolean bMissed) {
        //need a special resolution to check for any tele-missiles, deploy them, and 
        //take them out of the relevant attack value
        for(int wId: weapon.getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            Mounted bayWAmmo = bayW.getLinked();
            AmmoType atype = (AmmoType) bayWAmmo.getType();
            if(!bayW.isBreached() && !bayW.isDestroyed() && !bayW.isJammed()
                    && bayWAmmo.getShotsLeft() > 0 
                    && atype.hasFlag(AmmoType.F_TELE_MISSILE)) {
                server.deployTeleMissile(ae, atype, wId, getCritMod(atype), vPhaseReport);  
                attackValue -= atype.getDamagePerShot();
            }
        }
                
        if(attackValue <= 0) {
            return true;
        }
        return false;
    }
    
}
