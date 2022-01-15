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
package megamek.common.weapons.battlearmor;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.MicroBombHandler;
import megamek.common.weapons.Weapon;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class CLBAMicroBomb extends Weapon {
    private static final long serialVersionUID = 1467436625346131281L;

    public CLBAMicroBomb() {
        super();
        name = "Bomb Rack (Micro)";
        setInternalName("CLBAMicroBomb");
        addLookupName("CLBAMicro Bomb");
        heat = 0;
        damage = DAMAGE_VARIABLE;
        rackSize = 2;
        ammoType = AmmoType.T_BA_MICRO_BOMB;
        shortRange = 0;
        mediumRange = 0;
        longRange = 0;
        extremeRange = 0;
        bv = 11;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).or(F_ONESHOT).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        tonnage = .1;
        criticals = 2;
        cost = 30000;
        rulesRefs = "253, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3055, 3060, 3065, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CCC)
                .setProductionFactions(F_CCC);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              Server server) {
        return new MicroBombHandler(toHit, waa, game, server);
    }
}
