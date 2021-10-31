/**
 * MegaMek - Copyright (C) 2013 Ben Mazur (bmazur@sev.org)
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

public class ISTSEMPOneShot extends TSEMPWeapon {


    /**
     *
     */
    private static final long serialVersionUID = 2945503963826543215L;

    public ISTSEMPOneShot() {
        super();
        flags = flags.or(F_ONESHOT);
        cost = 500000;
        bv = 98;
        name = "TSEMP One-Shot";
        setInternalName(name);
        this.addLookupName("ISTSEMPOS");
        tonnage = 4;
        criticals = 3;
		rulesRefs = "90,IO";
		techAdvancement.setTechBase(TECH_BASE_IS)
			.setIntroLevel(false)
			.setUnofficial(false)
		    .setTechRating(RATING_E)
		    .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
		    .setISAdvancement(3090, 3095, DATE_NONE, DATE_NONE, DATE_NONE)
		    .setISApproximate(true, false, false,false, false)
		    .setPrototypeFactions(F_RS)
			.setProductionFactions(F_RS);
    }

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
        return new TSEMPOneShotHandler(toHit, waa, game, server);
    }
    */

}
