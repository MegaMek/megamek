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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class CLFireExtinguisher extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -5190894967392738394L;

    /**
     *
     */
    public CLFireExtinguisher() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_ADVANCED);
        name = "Fire Extinguisher";
        setInternalName(name);
        heat = 0;
        damage = 0;
        shortRange = 1;
        mediumRange = 1;
        longRange = 1;
        extremeRange = 1;
        tonnage = 0.0f;
        criticals = 0;
        flags = flags.or(F_SOLO_ATTACK).or(F_NO_FIRES);
        availRating = new int[] { EquipmentType.RATING_X,
                EquipmentType.RATING_B, EquipmentType.RATING_B };
        techRating = RATING_B;
        introDate = 2820;
        techLevel.put(2820, techLevel.get(3071));

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
        return new FireExtinguisherHandler(toHit, waa, game, server);
    }
}
