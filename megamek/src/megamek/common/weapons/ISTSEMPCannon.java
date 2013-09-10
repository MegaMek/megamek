/**
 * MegaMek - Copyright (C) 2013 Ben Mazur (bmazur@sev.org)
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

import megamek.common.TechConstants;


public class ISTSEMPCannon extends TSEMPWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -4861067053206502295L;

    public ISTSEMPCannon() {
        techLevel.put(3109,TechConstants.T_IS_ADVANCED);
        cost = 800000;
        bv = 488;
        name = "TSEMP Cannon";
        setInternalName(name);
        introDate = 3109;
        tonnage = 6;
        criticals  = 5;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     * megamek.server.Server)
     */
    /*@Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, IGame game, Server server) {
        return new TSEMPCannonHandler(toHit, waa, game, server);
    }
    */

}
