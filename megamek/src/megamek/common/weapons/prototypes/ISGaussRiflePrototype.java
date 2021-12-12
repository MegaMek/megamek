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
package megamek.common.weapons.prototypes;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PrototypeGaussHandler;
import megamek.common.weapons.gaussrifles.GaussWeapon;
import megamek.server.Server;

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
        name = "Prototype Gauss Rifle";
        setInternalName("ISGaussRiflePrototype");
        addLookupName("IS Gauss Rifle Prototype");
        shortName = "Gauss Rifle (P)";
        heat = 1;
        damage = 15;
        ammoType = AmmoType.T_GAUSS;
        minimumRange = 2;
        shortRange = 7;
        mediumRange = 15;
        longRange = 22;
        extremeRange = 30;
        tonnage = 15.0;
        bv = 320;
        cost = 1200000;
        criticals = 8;
        tankslots = 1;
        flags = flags.or(F_PROTOTYPE);
        explosionDamage = 20;
        rulesRefs = "71,IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_E)
        .setAvailability(RATING_F, RATING_F, RATING_X, RATING_X)
        .setISAdvancement(2587, DATE_NONE, DATE_NONE, 2590, 3038)
        .setISApproximate(false, false, false, true, true)
        .setPrototypeFactions(F_TH)
        .setProductionFactions(F_TH)
        .setReintroductionFactions(F_FS,F_LC)
        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

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
            WeaponAttackAction waa, Game game, Server server) {
        return new PrototypeGaussHandler(toHit, waa, game, server);
    }
}
