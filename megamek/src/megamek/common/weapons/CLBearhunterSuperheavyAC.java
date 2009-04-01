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

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class CLBearhunterSuperheavyAC extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -1042154309245048380L;

    /**
     *
     */
    public CLBearhunterSuperheavyAC() {
        super();
        techLevel = TechConstants.T_CLAN_TW;
        name = "Bearhunter Superheavy AC";
        setInternalName(name);
        addLookupName("CLBearhunter Superheavy AC");
        heat = 0;
        damage = 3;
        ammoType = AmmoType.T_NA;
        toHitModifier = 1;
        shortRange = 0;
        mediumRange = 1;
        longRange = 2;
        extremeRange = 2;
        tonnage = 0.0f;
        criticals = 0;
        bv = 4;
        flags |= F_DIRECT_FIRE | F_NO_FIRES | F_BALLISTIC | F_BA_WEAPON;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     *      megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new BearHunterHandler(toHit, waa, game, server);
    }
}
