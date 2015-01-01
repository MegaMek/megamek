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
/*
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class ArtilleryWeaponDirectFireHandler extends
        ArtilleryWeaponIndirectFireHandler {

    /**
     * 
     */
    private static final long serialVersionUID = 7116191142234200717L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public ArtilleryWeaponDirectFireHandler(ToHitData t, WeaponAttackAction w,
            IGame g, Server s) {
        super(t, w, g, s);

    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.AttackHandler#cares(int)
     */
    public boolean cares(IGame.Phase phase) {
        if (phase == IGame.Phase.PHASE_FIRING) {
            return true;
        }
        return false;
    }
}
