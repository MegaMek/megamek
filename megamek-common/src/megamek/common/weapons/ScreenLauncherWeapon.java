/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 25, 2004
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
 * @author Jay Lawson
 */
public class ScreenLauncherWeapon extends AmmoWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public ScreenLauncherWeapon() {

        this.techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        this.name = "Screen Launcher";
        this.setInternalName(this.name);
        this.addLookupName("ScreenLauncher");
        this.heat = 10;
        this.damage = 15;
        this.ammoType = AmmoType.T_SCREEN_LAUNCHER;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 40.0f;
        this.bv = 160;
        this.cost = 250000;
        this.shortAV = 15;
        this.maxRange = RANGE_SHORT;
        this.capital = false;
        this.atClass = CLASS_SCREEN;
        introDate = 3055;
        techLevel.put(3055, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        techRating = RATING_F;
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
        return new ScreenLauncherHandler(toHit, waa, game, server);
    }
}