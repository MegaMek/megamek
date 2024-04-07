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
import megamek.common.weapons.GRHandler;
import megamek.server.gameManager.*;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public class CLImprovedGaussRifle extends GaussWeapon {
    private static final long serialVersionUID = -8454131645293473685L;

    public CLImprovedGaussRifle() {
        super();
        name = "Improved Gauss Rifle";
        setInternalName("Improved Gauss Rifle");
        addLookupName("CLIMPGaussRifle");
        sortingName = "Gauss Imp";
        heat = 1;
        damage = 15;
        ammoType = AmmoType.T_GAUSS_IMP;
        minimumRange = 2;
        shortRange = 7;
        mediumRange = 15;
        longRange = 22;
        extremeRange = 30;
        tonnage = 13.0;
        criticals = 6;
        bv = 320;
        cost = 300000;
        shortAV = 15;
        medAV = 15;
        longAV = 15;
        maxRange = RANGE_LONG;
        explosionDamage = 20;
        rulesRefs = "96, IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_E, RATING_X, RATING_E)
                .setClanAdvancement(2818, 2821, 2822, 2837, 3080)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CGS)
                .setProductionFactions(F_CGS)
                .setReintroductionFactions(F_EI)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        return new GRHandler(toHit, waa, game, manager);
    }
}
