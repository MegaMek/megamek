/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.DamageType;

/**
 * @author Andrew Hunter
 * @since Oct 20, 2004
 */
public class NailRivetGunHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = 5635871269404561702L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public NailRivetGunHandler(ToHitData t, WeaponAttackAction w, Game g,
            Server s) {
        super(t, w, g, s);
        damageType = DamageType.NAIL_RIVET;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        return Compute.d6();
    }
}
