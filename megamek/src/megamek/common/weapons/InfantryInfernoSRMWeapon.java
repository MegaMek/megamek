/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class InfantryInfernoSRMWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 7788576728727248931L;

    public InfantryInfernoSRMWeapon() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "Infantry Inferno SRM";
        setInternalName(name);
        addLookupName("InfantryInfernoSRM");
        ammoType = AmmoType.T_SRM;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        // SRM Launcher (Standard, two-shot), TM p. 300
        cost = 1500;
        // SRM Launcher (Standard, two-shot) TM p. 319
        bv = 2.63;
        flags = flags.or(F_DIRECT_FIRE).or(F_INFERNO).or(F_MISSILE);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new InfantryInfernoSRMHandler(toHit, waa, game, server);
    }
}
