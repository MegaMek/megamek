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
public class ISMineLauncher extends Weapon {
    /**
     * 
     */
    private static final long serialVersionUID = -3445048091894801251L;

    /**
     * 
     */
    public ISMineLauncher() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Mine Launcher";
        this.setInternalName(BattleArmor.MINE_LAUNCHER);
        this.addLookupName("ISMine Launcher");
        this.heat = 0;
        this.damage = DAMAGE_SPECIAL;
        this.rackSize = 1;
        this.ammoType = AmmoType.T_MINE;
        this.shortRange = 0;
        this.mediumRange = 0;
        this.longRange = 0;
        this.extremeRange = 0;
        this.tonnage = 0.0f;
        this.criticals = 0;
        this.bv = 6;
        String[] modes = { "Single", "2-shot", "3-shot", "4-shot" };
        this.setModes(modes);
        this.flags |= F_DIRECT_FIRE | F_SOLO_ATTACK;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     *      megamek.server.Server)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new MineLauncherHandler(toHit, waa, game, server);
    }
}
