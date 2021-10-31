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
import megamek.common.weapons.AmmoBayWeaponHandler;
import megamek.common.weapons.AttackHandler;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class AMSBayWeapon extends AmmoBayWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public AMSBayWeapon() {
        super();
        // tech levels are a little tricky
        this.name = "AMS Bay";
        this.setInternalName(EquipmentTypeLookup.AMS_BAY);
        this.heat = 0;
        this.damage = DAMAGE_VARIABLE;
        this.shortRange = 1;
        this.tonnage = 0.0;
        this.bv = 0;
        this.cost = 0;
        this.atClass = CLASS_AMS;
		flags = flags.or(F_AUTO_TARGET).or(F_AMSBAY).or(F_AERO_WEAPON);
		setModes(new String[] { "On", "Off" });
		setInstantModeSwitch(false);
		techAdvancement.setTechBase(TECH_BASE_ALL)
		.setTechRating(RATING_E)
		.setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
		.setISAdvancement(2613, 2617, 3048, 2835, 3045)
		.setISApproximate(true, false, false,false, false)
		.setClanAdvancement(2824, 2831, 2835, DATE_NONE, DATE_NONE)
		.setClanApproximate(true, false, false, false, false)
		.setPrototypeFactions(F_TH,F_CSA)
		.setProductionFactions(F_TH,F_CSA)
		.setReintroductionFactions(F_CC)
		.setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
    
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, Server server) {
        return new AmmoBayWeaponHandler(toHit, waa, game, server);
    }
}


