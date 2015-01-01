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
 * @author Sebastian Brocks
 */
public class CLMicroBomb extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = 1467436625346131281L;

    /**
     *
     */
    public CLMicroBomb() {
        super();
        techLevel = TechConstants.T_CLAN_TW;
        name = "Micro Bomb";
        setInternalName("CLMicroBomb");
        addLookupName("CLMicro Bomb");
        heat = 0;
        damage = DAMAGE_VARIABLE;
        rackSize = 2;
        ammoType = AmmoType.T_BA_MICRO_BOMB;
        shortRange = 0;
        mediumRange = 0;
        longRange = 0;
        extremeRange = 0;
        bv = 11;
        flags |= F_NO_FIRES;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new MicroBombHandler(toHit, waa, game, server);
    }
}
