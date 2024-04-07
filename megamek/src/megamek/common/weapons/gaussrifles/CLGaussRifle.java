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
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.GRHandler;
import megamek.server.gameManager.*;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public class CLGaussRifle extends GaussWeapon {
    private static final long serialVersionUID = -4436936560457546313L;

    public CLGaussRifle() {
        super();

        this.name = "Gauss Rifle";
        this.setInternalName("CLGaussRifle");
        this.addLookupName("Clan Gauss Rifle");
        this.heat = 1;
        this.damage = 15;
        this.ammoType = AmmoType.T_GAUSS;
        this.minimumRange = 2;
        this.shortRange = 7;
        this.mediumRange = 15;
        this.longRange = 22;
        this.extremeRange = 30;
        this.tonnage = 12.0;
        this.criticals = 6;
        this.bv = 320;
        this.cost = 300000;
        this.shortAV = 15;
        this.medAV = 15;
        this.longAV = 15;
        this.maxRange = RANGE_LONG;
        this.explosionDamage = 20;
        rulesRefs = "219, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_F, RATING_D, RATING_D)
                .setClanAdvancement(2822, 2828, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CBR)
                .setProductionFactions(F_CBR);
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
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        return new GRHandler(toHit, waa, game, manager);
    }
}
