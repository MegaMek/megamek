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
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class TeleMissileHandler extends AmmoWeaponHandler {

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
    public TeleMissileHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /**
     * handle this weapons firing
     * 
     * @return a <code>boolean</code> value indicating wether this should be
     *         kept or not
     */
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        
        AmmoType atype = (AmmoType)ammo.getType();
        //just launch the tele-missile
        server.deployTeleMissile(ae, atype, ae.getEquipmentNum(weapon), getCapMisMod(), vPhaseReport);      
        
        return false;
        
    }
    
}