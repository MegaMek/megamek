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
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jason Tighe
 */
public class ACFlakHandler extends LBXHandler {

    /**
     *
     */
    private static final long serialVersionUID = -7814754695629391969L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public ACFlakHandler(ToHitData t, WeaponAttackAction w, Game g, Server s) {
        super(t, w, g, s);
        sSalvoType = " fragment(s) ";
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    @Override
    protected int calcnCluster() {
        return 5;
    }

    @Override
    protected boolean usesClusterTable() {
        return ((AmmoType) ammo.getType()).getMunitionType() == AmmoType.M_FLAK;
    }

}
