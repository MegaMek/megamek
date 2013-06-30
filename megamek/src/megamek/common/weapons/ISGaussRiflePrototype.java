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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.common.EquipmentType;

/**
 * @author Sebastian Brocks
 */
public class ISGaussRiflePrototype extends GaussWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 317770140657000258L;

    /**
     *
     */
    public ISGaussRiflePrototype() {
        super();
        techLevel.put(2587, TechConstants.T_IS_EXPERIMENTAL);
        name = "Gauss Rifle Prototype";
        setInternalName("ISGaussRiflePrototype");
        addLookupName("IS Gauss Rifle Prototype");
        heat = 1;
        damage = 15;
        ammoType = AmmoType.T_GAUSS;
        minimumRange = 2;
        shortRange = 7;
        mediumRange = 15;
        longRange = 22;
        extremeRange = 30;
        tonnage = 15.0f;
        bv = 320;
        cost = 300000;
        criticals = 8;
        flags = flags.or(F_PROTOTYPE);
        explosionDamage = 20;
        introDate = 2587;
        extinctDate = 2590;
        reintroDate = 3040;
        availRating = new int[] { RATING_E,RATING_F, RATING_D };
        techRating = RATING_E;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new PrototypeGaussHandler(toHit, waa, game, server);
    }
}
