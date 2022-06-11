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
package megamek.common.weapons.prototypes;

import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.SmallPulseLaserPrototypeHandler;
import megamek.common.weapons.lasers.PulseLaserWeapon;
import megamek.server.GameManager;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class ISPulseLaserSmallPrototype extends PulseLaserWeapon {
    private static final long serialVersionUID = 2977404162226570144L;

    public ISPulseLaserSmallPrototype() {
        super();
        this.name = "Prototype Small Pulse Laser";
        this.setInternalName("ISSmallPulseLaserPrototype");
        this.addLookupName("IS Prototype Small Pulse Laser");
        this.addLookupName("ISSmall Pulse Laser Prototype");
        shortName = "Small Pulse Laser (P)";
        this.heat = 2;
        this.damage = 3;
        this.infDamageClass = WeaponType.WEAPON_BURST_2D6;
        this.toHitModifier = -1;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.waterShortRange = 1;
        this.waterMediumRange = 2;
        this.waterLongRange = 2;
        this.waterExtremeRange = 4;
        this.tonnage = 1.0;
        this.criticals = 1;
        this.bv = 12;
        this.cost = 80000;
        this.shortAV = 3;
        this.maxRange = RANGE_SHORT;
        this.atClass = CLASS_POINT_DEFENSE;
        this.flags = flags.or(F_BURST_FIRE).or(F_PROTOTYPE);
        rulesRefs = "71, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2595, DATE_NONE, DATE_NONE, 2609, DATE_NONE)
                .setISApproximate(false, false, false, true, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
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
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        return new SmallPulseLaserPrototypeHandler(toHit, waa, game, manager);
    }
}
