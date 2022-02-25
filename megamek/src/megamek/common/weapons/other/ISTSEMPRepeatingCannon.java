/**
 * MegaMek - Copyright (C) 2016 Megamek Team
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
package megamek.common.weapons.other;

import megamek.common.SimpleTechLevel;

public class ISTSEMPRepeatingCannon extends TSEMPWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -4861067053206502295L;

    public ISTSEMPRepeatingCannon() {
        cost = 1200000;
        bv = 600;
        name = "TSEMP Repeating Cannon";
        setInternalName(name);
        this.addLookupName("ISTSEMPREPEATING");
        flags = flags.or(F_TSEMP).or(F_DIRECT_FIRE).or(F_REPEATING);
        tonnage = 8;
        criticals  = 7;
        tankslots = 1;
		rulesRefs = "92, IO";
		techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_F)
		    .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
		    .setISAdvancement(3133, DATE_NONE, DATE_NONE, 3138, DATE_NONE)
		    .setPrototypeFactions(F_RS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
    
    //TODO - Implement Game Rules.  See IO pg 94 for specifics.

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     * megamek.server.Server)
     */
    /*@Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game, Server server) {
        return new TSEMPCannonHandler(toHit, waa, game, server);
    }
    */

}
