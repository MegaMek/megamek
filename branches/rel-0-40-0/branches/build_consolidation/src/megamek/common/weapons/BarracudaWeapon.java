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
public class BarracudaWeapon extends CapitalMissileWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public BarracudaWeapon() {
        super();
        this.techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        this.name = "Barracuda";
        this.setInternalName(this.name);
        this.addLookupName("Barracuda");
        this.heat = 10;
        this.damage = 2;
        this.ammoType = AmmoType.T_BARRACUDA;
        this.shortRange = 20;
        this.mediumRange = 30;
        this.longRange = 40;
        this.extremeRange = 50;
        this.tonnage = 90.0f;
        this.bv = 522;
        this.cost = 90000;
        this.atClass = CLASS_CAPITAL_MISSILE;
        this.shortAV = 2;
        this.medAV = 2;
        this.longAV = 2;
        this.extAV = 2;
        this.maxRange = RANGE_EXT;
        this.toHitModifier = -2;
        introDate = 2305;
        techLevel.put(2305, techLevel.get(3071));
        extinctDate = 2855;
        reintroDate = 3051;
        availRating = new int[] { RATING_D, RATING_X, RATING_D };
        techRating = RATING_F;

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
        return new BarracudaHandler(toHit, waa, game, server);
    }
}
