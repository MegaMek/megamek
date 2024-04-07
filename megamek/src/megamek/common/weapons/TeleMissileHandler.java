/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.server.GameManager;
import org.apache.logging.log4j.LogManager;

import java.util.Vector;

/**
 * @author Jay Lawson
 */
public class TeleMissileHandler extends CapitalMissileBayHandler {
    private static final long serialVersionUID = -1618484541772117621L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public TeleMissileHandler(ToHitData t, WeaponAttackAction w, Game g,
            GameManager m) {
        super(t, w, g, m);
    }
    
    private int missileArmor = 0;
    
    /**
     * Method that collects the linked ammo type for a weapon bay
     * We need this to pass through to server without using the ammo
     * in the process
     */
    private AmmoType getBayAmmoType() {
        AmmoType at = null;
        for (int wId : weapon.getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            // check the currently loaded ammo
            Mounted bayWAmmo = bayW.getLinked();

            if (bayWAmmo == null) {
                LogManager.getLogger().debug("Handler can't find any ammo! Oh no!");
                continue;
            }
             //Once we have some ammo to send to the server, stop looking
             at = (AmmoType) bayWAmmo.getType();
             break;
        }
        return at;
    }
    
    private int calcBayDamageAndHeat() {
        int damage = 0;
        for (int wId : weapon.getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            WeaponType bayWType = ((WeaponType) bayW.getType());
            damage += (int) bayWType.getShortAV();
            ae.heatBuildup += bayW.getCurrentHeat();
            missileArmor = bayWType.getMissileArmor();
        }
        return damage;
    }
    
    @Override
    protected void useAmmo() {
        for (int wId : weapon.getBayWeapons()) {
            Mounted bayW = ae.getEquipment(wId);
            // check the currently loaded ammo
            Mounted bayWAmmo = bayW.getLinked();

            if (bayWAmmo == null) {// Can't happen. w/o legal ammo, the weapon
                // *shouldn't* fire.
                LogManager.getLogger().debug("Handler can't find any ammo! Oh no!");
            }
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
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        // just launch the tele-missile
        gameManager.deployTeleMissile(ae, wtype, getBayAmmoType(), ae.getEquipmentNum(weapon),
                getCapMisMod(), calcBayDamageAndHeat(), missileArmor, vPhaseReport);

        return false;

    }

}