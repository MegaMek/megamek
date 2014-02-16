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
import megamek.common.BattleArmor;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PopUpMineLauncherHandler;
import megamek.common.weapons.Weapon;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class ISBAPopUpMineLauncher extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -3445048091894801251L;

    /**
     *
     */
    public ISBAPopUpMineLauncher() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Pop-up Mine";
        setInternalName(BattleArmor.MINE_LAUNCHER);
        addLookupName("ISMine Launcher");
        heat = 0;
        damage = DAMAGE_SPECIAL;
        rackSize = 1;
        ammoType = AmmoType.T_MINE;
        shortRange = 0;
        mediumRange = 0;
        longRange = 0;
        extremeRange = 0;
        tonnage = 0.2f;
        criticals = 1;
        cost = 2500;
        bv = 6;
        String[] modeStrings = { "Single", "2-shot", "3-shot", "4-shot" };
        setModes(modeStrings);
        flags = flags.or(F_DIRECT_FIRE).or(F_SOLO_ATTACK).or(F_BA_WEAPON);
        introDate = 3050;
        techLevel.put(3050, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        techRating = RATING_E;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new PopUpMineLauncherHandler(toHit, waa, game, server);
    }
}
