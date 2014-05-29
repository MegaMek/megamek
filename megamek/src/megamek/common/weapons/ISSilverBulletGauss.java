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
 * Created on Oct 15, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class ISSilverBulletGauss extends GaussWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -6873790245999096707L;

    /**
     *
     */
    public ISSilverBulletGauss() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Silver Bullet Gauss Rifle";
        setInternalName("ISSBGR");
        addLookupName("IS Silver Bullet Gauss Rifle");
        addLookupName("ISSBGaussRifle");
        heat = 1;
        damage = 15;
        rackSize = 15;
        minimumRange = 2;
        shortRange = 7;
        mediumRange = 15;
        longRange = 22;
        extremeRange = 30;
        tonnage = 15.0f;
        criticals = 7;
        bv = 198;
        cost = 350000;
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        maxRange = RANGE_LONG;
        ammoType = AmmoType.T_SBGAUSS;
        // SB Gauss rifles can neither benefit from a targeting computer nor
        // do they add to its mass and size (TacOps pp. 314/5); thus, the
        // "direct fire" flag inherited from the superclass needs to go again.
        flags = flags.or(F_NO_AIM).andNot(F_DIRECT_FIRE);
        atClass = CLASS_LBX_AC;
        explosionDamage = 20;
        techRating = RATING_E;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        introDate = 3051;
        techLevel.put(3051, techLevel.get(3071));
        techLevel.put(3080, TechConstants.T_IS_TW_NON_BOX);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new LBXHandler(toHit, waa, game, server);
    }

}
