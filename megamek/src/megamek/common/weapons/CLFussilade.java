/*
 * MegaMek - Copyright (C) 2011 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons;

import megamek.common.BattleForceElement;
import megamek.common.TechConstants;
import megamek.common.TechProgression;
import megamek.common.WeaponType;

/**
 * @author beerockxs
 * 
 */
public class CLFussilade extends ATMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 1237937853765733086L;

    public CLFussilade() {
        super();
        techLevel.clear();
        flags = flags.or(WeaponType.F_PROTO_WEAPON);
        rackSize = 3;
        minimumRange = 4;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        damage = 6;
        shortAV = 6;
        medAV = 6;
        longAV = 6;
        maxRange = RANGE_MED;
        techRating = RATING_F;
        cost = 100000;
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        bv = 11;
        introDate = 3072;
        techLevel.put(3072, TechConstants.T_CLAN_EXPERIMENTAL);

        techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        techProgression.setClanProgression(3072);
        techProgression.setTechRating(RATING_F);
        techProgression.setAvailability(new int[] { RATING_X, RATING_X, RATING_F, RATING_X });
    }

    @Override
    public double getBattleForceDamage(int range) {
        double damage = 0;
        if (range <= getLongRange()) {
            damage = getRackSize();
            if (range < BattleForceElement.MEDIUM_RANGE) {
                damage *= 3;
            } else if (range < BattleForceElement.LONG_RANGE) {
                damage *= 2;
            }
            if (range == BattleForceElement.SHORT_RANGE && getMinimumRange() > 0) {
                damage = adjustBattleForceDamageForMinRange(damage);
            }
        }
        return damage / 10.0;
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_IATM;
    }
}
