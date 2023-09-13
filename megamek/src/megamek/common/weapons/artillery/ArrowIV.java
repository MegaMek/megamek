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
 * @author Martin Metke
 * @since Sep 12, 2023
 */


public abstract class ArrowIV extends ArtilleryWeapon {
    private static final long serialVersionUID = -4495524659692575107L;

    // Air-Defense Arrow IV (ADA) missile ranges differ from normal Arrow IV ammo
    public final int ADA_MIN_RANGE = 0;
    public final int ADA_SHORT_RANGE = 17;
    public final int ADA_MED_RANGE = 34;
    public final int ADA_LONG_RANGE = 51;
    public final int ADA_EXT_RANGE = 51;

    public ArrowIV() {
        super();

        name = "Arrow IV";
        setInternalName("ArrowIV");
        addLookupName("ArrowIVSystem");
        addLookupName("Arrow IV System");
        addLookupName("Arrow IV Missile System");
        heat = 10;
        rackSize = 20;
        ammoType = AmmoType.T_ARROW_IV;
        bv = 240;
        cost = 450000;
        this.flags = flags.or(F_MISSILE);
        this.missileArmor = 20;
        rulesRefs = "284, TO";
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
                minRange = ADA_MIN_RANGE;
                sRange = ADA_SHORT_RANGE;
                mRange = ADA_MED_RANGE;
                lRange = ADA_LONG_RANGE;
                eRange = ADA_EXT_RANGE;
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
