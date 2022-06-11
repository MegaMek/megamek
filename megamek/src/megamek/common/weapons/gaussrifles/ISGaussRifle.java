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
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.ASRange;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.GRHandler;
import megamek.server.GameManager;

import java.math.BigDecimal;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public class ISGaussRifle extends GaussWeapon {
    private static final long serialVersionUID = -8454131645293473685L;

    public ISGaussRifle() {
        super();

        name = "Gauss Rifle";
        setInternalName("ISGaussRifle");
        addLookupName("IS Gauss Rifle");
        sortingName = "Gauss C";
        heat = 1;
        damage = 15;
        ammoType = AmmoType.T_GAUSS;
        minimumRange = 2;
        shortRange = 7;
        mediumRange = 15;
        longRange = 22;
        extremeRange = 30;
        tonnage = 15.0;
        criticals = 7;
        bv = 320;
        cost = 300000;
        shortAV = 15;
        medAV = 15;
        longAV = 15;
        maxRange = RANGE_LONG;
        explosionDamage = 20;
        rulesRefs = "219, TM";
        flags = flags.andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_D, RATING_C)
                .setISAdvancement(2587, 2590, 3045, 2865, 3040)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_FC, F_FW, F_DC);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        return new GRHandler(toHit, waa, game, manager);
    }

    @Override
    public double getBattleForceDamage(int range) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 1.245;
        } else if (range < AlphaStrikeElement.EXTREME_RANGE) {
            return 1.5;
        } else {
            return 0;
        }
    }

    @Override
    public BigDecimal getAlphaStrikeDamage(ASRange range) {
        switch (range) {
            case SHORT:
                return new BigDecimal("1.245");
            case MEDIUM:
            case LONG:
                return new BigDecimal("1.5");
            default:
                return BigDecimal.ZERO;
        }
    }
}
