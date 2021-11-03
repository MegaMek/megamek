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
package megamek.common.weapons.bayweapons;

import megamek.common.EquipmentTypeLookup;
import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.MissileBayWeaponHandler;
import megamek.common.weapons.AttackHandler;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class ATMBayWeapon extends AmmoBayWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public ATMBayWeapon() {
        super();
        // tech levels are a little tricky
        this.name = EquipmentTypeLookup.ATM_BAY;
        this.setInternalName(this.name);
        this.heat = 0;
        this.damage = DAMAGE_VARIABLE;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 20;
        this.extremeRange = 25;
        this.tonnage = 0.0;
        this.bv = 0;
        this.cost = 0;
        this.flags = flags.or(F_MISSILE);
        this.maxRange = RANGE_SHORT;
        this.atClass = CLASS_ATM;
        // Using same TA as ATM weapon
        techAdvancement.setTechBase(TECH_BASE_CLAN) .setTechRating(RATING_F)
        	.setAvailability(RATING_X, RATING_X, RATING_D, RATING_D).setClanAdvancement(3052, 3053, 3054)
        	.setClanApproximate(true, true, true).setPrototypeFactions(F_CCY)
        	.setProductionFactions(F_CCY).setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
    
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, Server server) {
        return new MissileBayWeaponHandler(toHit, waa, game, server);
    }
}
