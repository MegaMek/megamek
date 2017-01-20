/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.battlearmor;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.Weapon;
import megamek.server.Server;

public class CLBALBX extends Weapon {

    /**
     *
     */
    private static final long serialVersionUID = 2978911783244524588L;

    public CLBALBX() {
        super();

        name = "Battle Armor LB-X AC";
        setInternalName(name);
        addLookupName("CLBALBX");
        heat = 0;
        damage = 4;
        rackSize = 4;
        shortRange = 2;
        mediumRange = 5;
        longRange = 8;
        extremeRange = 10;
        tonnage = 0.4f;
        criticals = 2;
        toHitModifier = -1;
        ammoType = AmmoType.T_NA;
        bv = 20;
        cost = 70000;
        introDate = 3075;
        techLevel.put(3075, TechConstants.T_CLAN_EXPERIMENTAL);
        techLevel.put(3085, TechConstants.T_CLAN_ADVANCED);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_E ,RATING_D};
        techRating = RATING_F;
        rulesRefs = "207, TM";
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new BALBXHandler(toHit, waa, game, server);
    }

}
