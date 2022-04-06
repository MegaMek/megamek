/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.weapons.Weapon;

/**
 * @author Sebastian Brocks
 * @since Sep 25, 2004
 */
public class ISBAAPDS extends Weapon {
    private static final long serialVersionUID = 5678281956614161074L;

    public ISBAAPDS() {
        super();
        name = "RISC Advanced Point Defense System";
        setInternalName("ISBAAPDS");
        tonnage = 0.35;
        criticals = 2;
        bv = 7;
        longRange = 3;
		flags = flags.or(F_BA_WEAPON).or(F_AUTO_TARGET).or(F_AMS).or(F_BALLISTIC).andNot(F_MECH_WEAPON)
				.andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        setModes(new String[] { "On", "Off" });
        setInstantModeSwitch(false);
        cost = 50000;
        rulesRefs = "91, IO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3132, 3134, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_RS)
                .setProductionFactions(F_RS);
    }
}
