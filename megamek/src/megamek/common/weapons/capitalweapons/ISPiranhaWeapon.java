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
package megamek.common.weapons.capitalweapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PiranhaHandler;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class ISPiranhaWeapon extends SubCapitalMissileWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 3827228773282489872L;

    /**
     * 
     */
    public ISPiranhaWeapon() {
        super();
        name = "Sub-Capital Missile Launcher (Piranha)";
        setInternalName(name);
        addLookupName("Piranha");
        heat = 9;
        damage = 3;
        ammoType = AmmoType.T_PIRANHA;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 100.0f;
        bv = 670;
        cost = 75000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        maxRange = RANGE_LONG;
        flags = flags.or(F_AERO_WEAPON).or(F_MISSILE);
		rulesRefs = "345,TO";
		techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
		        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
		        .setISAdvancement(3066, 3072, 3145, DATE_NONE, DATE_NONE)
		        .setISApproximate(true, false, false, false, false)
		        .setClanAdvancement(DATE_NONE, DATE_NONE, 3073, DATE_NONE, DATE_NONE)
		        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_WB, F_CGB, F_CNC)
		        .setProductionFactions(F_WB);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new PiranhaHandler(toHit, waa, game, server);
    }
}
