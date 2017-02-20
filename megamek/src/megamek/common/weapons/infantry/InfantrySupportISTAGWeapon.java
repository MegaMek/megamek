/**
 * MegaMek - Copyright (C) 2017 The MegaMek team
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
package megamek.common.weapons.infantry;

import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.TAGHandler;
import megamek.server.Server;

/**
 * TAG for conventional infantry. Rules not found in TacOps 2nd printing are in this forum post:
 * http://bg.battletech.com/forums/index.php?topic=5902.0
 * 
 * @author Neoancient
 *
 */
public class InfantrySupportISTAGWeapon extends InfantryWeapon {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4986981464279987117L;

	public InfantrySupportISTAGWeapon() {
        super();
        flags = flags.andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON)
        		.andNot(F_TANK_WEAPON).andNot(F_BA_WEAPON).andNot(F_PROTO_WEAPON)
                .or(F_TAG).or(F_NO_FIRES).or(F_INF_ENCUMBER);

        name = "TAG (Light, Man-Portable)";
        setInternalName("InfantryTAG");
        addLookupName("Infantry TAG");
        damage = 0;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        cost = 40000;
    	introDate = 2593;
    	extinctDate = 2770;
    	reintroDate = 3051;
    	techLevel.put(2593, TechConstants.T_IS_EXPERIMENTAL);
    	techLevel.put(2610, TechConstants.T_IS_ADVANCED);
    	techLevel.put(3051, TechConstants.T_IS_EXPERIMENTAL);
    	techLevel.put(3061, TechConstants.T_IS_ADVANCED);
    	availRating = new int[] { RATING_F,RATING_X ,RATING_E ,RATING_E};
    	techRating = RATING_E;
    	rulesRefs = "273, TM";
	}

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new TAGHandler(toHit, waa, game, server);
    }
}
