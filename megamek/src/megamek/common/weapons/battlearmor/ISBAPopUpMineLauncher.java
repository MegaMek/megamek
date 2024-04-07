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
import megamek.common.BattleArmor;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PopUpMineLauncherHandler;
import megamek.common.weapons.Weapon;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public class ISBAPopUpMineLauncher extends Weapon {
    private static final long serialVersionUID = -3445048091894801251L;

    public ISBAPopUpMineLauncher() {
        super();
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
        tonnage = 0.2;
        criticals = 1;
        cost = 2500;
        bv = 6;
        String[] modeStrings = { "Single", "2-shot", "3-shot", "4-shot", "5-shot", "6-shot" };
        setModes(modeStrings);
        flags = flags.or(F_DIRECT_FIRE).or(F_SOLO_ATTACK).or(F_BA_WEAPON).or(F_ONESHOT).or(F_BA_INDIVIDUAL);
        rulesRefs = "267, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
    	        .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_F)
                .setISAdvancement(DATE_NONE, 3050, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC);
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
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        return new PopUpMineLauncherHandler(toHit, waa, game, manager);
    }
}
