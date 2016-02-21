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

import megamek.common.BattleArmor;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.server.Compute;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class SRMHandler extends MissileWeaponHandler {

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
    public SRMHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        this(t, w, g, s, 0);
    }

    public SRMHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s,
            int salvoMod) {
        super(t, w, g, s);
        nSalvoBonus = salvoMod;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            double toReturn = Compute.directBlowInfantryDamage(
                    wtype.getRackSize() * 2, bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized());
            if (bGlancing) {
                toReturn /= 2;
            }
            return (int) Math.floor(toReturn);
        }
        return 2;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    @Override
    protected int calcnCluster() {
        return 1;
    }

}
