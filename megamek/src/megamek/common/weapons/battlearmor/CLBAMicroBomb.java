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
package megamek.common.weapons.battlearmor;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.TechAdvancement;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.MicroBombHandler;
import megamek.common.weapons.Weapon;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class CLBAMicroBomb extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = 1467436625346131281L;

    /**
     *
     */
    public CLBAMicroBomb() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
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
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        tonnage = .1f;
        criticals = 2;
        cost = 30000;
        introDate = 3050;
        techLevel.put(3050, TechConstants.T_CLAN_EXPERIMENTAL);
        techLevel.put(3060, TechConstants.T_CLAN_ADVANCED);
        techLevel.put(3065, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_F ,RATING_E};
        techRating = RATING_F;
        rulesRefs = "253, TM";

        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(3050, 3060, 3065);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_E });
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new MicroBombHandler(toHit, waa, game, server);
    }
}
