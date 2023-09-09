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
package megamek.common.weapons.artillery;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.ADAMissileWeaponHandler;
import megamek.common.weapons.AttackHandler;
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class ISArrowIV extends ArtilleryWeapon {
    private static final long serialVersionUID = -4495524659692575107L;

    public ISArrowIV() {
        super();

        name = "Arrow IV";
        setInternalName("ISArrowIV");
        addLookupName("ISArrowIVSystem");
        addLookupName("IS Arrow IV System");
        addLookupName("IS Arrow IV Missile System");
        heat = 10;
        rackSize = 20;
        ammoType = AmmoType.T_ARROW_IV;
        shortRange = 1;
        mediumRange = 2;
        longRange = 8;
        extremeRange = 8; // No extreme range.
        tonnage = 15;
        criticals = 15;
        svslots = 7;
        bv = 240;
        cost = 450000;
        this.flags = flags.or(F_MISSILE);
        this.missileArmor = 20;
        rulesRefs = "284, TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2593, 2600, DATE_NONE, 2830, 3044)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    public int[] getRanges(Mounted weapon) {
        // modify the ranges for Arrow missile systems based on the ammo selected
        int minRange = getMinimumRange();
        int sRange = getShortRange();
        int mRange = getMediumRange();
        int lRange = getLongRange();
        int eRange = getExtremeRange();
        boolean hasLoadedAmmo = (weapon.getLinked() != null);
        if (hasLoadedAmmo) {
            AmmoType atype = (AmmoType) weapon.getLinked().getType();
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_ADA)) {
                minRange = 0;
                sRange = 17;
                mRange = 34;
                lRange = 51;
                eRange = 51;
            }
        }
        return new int[] { minRange, sRange, mRange, lRange, eRange };
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
                                              WeaponAttackAction waa, Game game, GameManager manager) {
        if(waa.getAmmoMunitionType().contains(AmmoType.Munitions.M_ADA)){
            return new ADAMissileWeaponHandler(toHit, waa, game, manager);
        }
        return super.getCorrectHandler(toHit, waa, game, manager);
    }
}
