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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons.gaussrifles;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.GRHandler;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class CLImprovedGaussRifle extends GaussWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -8454131645293473685L;

    /**
     * 
     */
    public CLImprovedGaussRifle() {
        super();

        this.name = "Improved Gauss Rifle";
        this.setInternalName("Improved Gauss Rifle");
        this.addLookupName("CLIMPGaussRifle");
        this.heat = 1;
        this.damage = 15;
        this.ammoType = AmmoType.T_GAUSS_IMP;
        this.minimumRange = 2;
        this.shortRange = 7;
        this.mediumRange = 15;
        this.longRange = 22;
        this.extremeRange = 30;
        this.tonnage = 13.0;
        this.criticals = 6;
        this.bv = 320;
        this.cost = 300000;
        this.shortAV = 15;
        this.medAV = 15;
        this.longAV = 15;
        this.maxRange = RANGE_LONG;
        this.explosionDamage = 20;
        rulesRefs = "96, IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_E, RATING_X, RATING_E)
            .setClanAdvancement(2818, 2821, 2822, 2837, 3080)
            .setClanApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_CGS).setProductionFactions(F_CGS)
            .setReintroductionFactions(F_EI).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        return new GRHandler(toHit, waa, game, server);
    }

}
