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
import megamek.common.BattleArmor;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class ISPopUpMineLauncher extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -3445048091894801251L;

    /**
     *
     */
    public ISPopUpMineLauncher() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
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
        tonnage = 0.0f;
        criticals = 0;
        bv = 6;
        String[] modeStrings =
            { "Single", "2-shot", "3-shot", "4-shot" };
        setModes(modeStrings);
        flags = flags.or(F_DIRECT_FIRE).or(F_SOLO_ATTACK).or(F_BA_WEAPON);
        introDate = 3062;
        availRating = new int[]{RATING_X,RATING_X,RATING_F};
        techRating = RATING_D;
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
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, IGame game, Server server) {
        return new PopUpMineLauncherHandler(toHit, waa, game, server);
    }
}
