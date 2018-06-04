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
public class TeleMissileHandler extends CapitalMissileBayHandler {

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
    public TeleMissileHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }
    private AmmoType at = null;
    
    @Override
    protected void useAmmo() {
        final String METHOD_NAME = "useAmmo()";
        for (int wId : weapon.getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            // check the currently loaded ammo
            Mounted bayWAmmo = bayW.getLinked();

            if (bayWAmmo == null) {// Can't happen. w/o legal ammo, the weapon
                // *shouldn't* fire.
                logDebug(METHOD_NAME, "Handler can't find any ammo! Oh no!");
            }
            at = (AmmoType) bayWAmmo.getType();
            int shots = bayW.getCurrentShots();
            for (int i = 0; i < shots; i++) {
                if (null == bayWAmmo
                        || bayWAmmo.getUsableShotsLeft() < 1) {
                    // try loading something else
                    ae.loadWeaponWithSameAmmo(bayW);
                    bayWAmmo = bayW.getLinked();
                }
                if (null != bayWAmmo) {
                    bayWAmmo.setShotsLeft(bayWAmmo.getBaseShotsLeft() - 1);
                }
            }
        }
    }

    /**
     * handle this weapons firing
     * 
     * @return a <code>boolean</code> value indicating wether this should be
     *         kept or not
     */
    @Override
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        final String METHOD_NAME = "handle()";
        useAmmo();        
        if (at == null) {
            logDebug(METHOD_NAME, "AmmoType is null!");
        }
        // just launch the tele-missile
        server.deployTeleMissile(ae, at, ae.getEquipmentNum(weapon),
                getCapMisMod(), vPhaseReport);

        return false;

    }

}