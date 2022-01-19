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
package megamek.common.weapons.gaussrifles;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.HGRHandler;
import megamek.server.Server;

/**
 * @author Jason Tighe
 * @since Oct 19, 2004
 */
public class ISImpHGaussRifle extends GaussWeapon {
    private static final long serialVersionUID = -2379383217525139478L;

    public ISImpHGaussRifle() {
        super();

        name = "Improved Heavy Gauss Rifle";
        shortName = "Imp. Heavy Gauss Rifle";
        setInternalName("ISImprovedHeavyGaussRifle");
        addLookupName("IS Improved Heavy Gauss Rifle");
        sortingName = "Gauss IMP D";
        heat = 2;
        damage = 22;
        ammoType = AmmoType.T_IGAUSS_HEAVY;
        minimumRange = 3;
        shortRange = 6;
        mediumRange = 12;
        longRange = 19;
        extremeRange = 24;
        tonnage = 20.0;
        criticals = 11;
        bv = 385;
        cost = 700000;
        shortAV = 22;
        medAV = 22;
        longAV = 22;
        maxRange = RANGE_LONG;
        explosionDamage = 30;
        rulesRefs = "313, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3065, DATE_NONE, 3081, DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_LC).setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              Server server) {
        return new HGRHandler(toHit, waa, game, server);
    }
}
