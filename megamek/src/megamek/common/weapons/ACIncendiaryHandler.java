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
 * Created on Sep 5, 2005
 *
 */
package megamek.common.weapons;

import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Sebastian Brocks
 */
public class ACIncendiaryHandler extends ACWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = 3301631731286472616L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public ACIncendiaryHandler(ToHitData t, WeaponAttackAction w, IGame g,
                               Server s) {
        super(t, w, g, s);
        damageType = DamageType.INCENDIARY;
    }

}
