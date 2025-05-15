/*
 * MegaMek - Copyright (C) 2013 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.other;

import megamek.common.SimpleTechLevel;

public class ISTSEMPOneShot extends TSEMPWeapon {

    private static final long serialVersionUID = 2945503963826543215L;

    public ISTSEMPOneShot() {
        super();
        flags = flags.or(F_ONESHOT);
        cost = 500000;
        bv = 98;
        name = "TSEMP One-Shot";
        setInternalName(name);
        addLookupName("ISTSEMPOS");
        tonnage = 4;
        criticals = 3;
		rulesRefs = "84, IO:AE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
		techAdvancement.setTechBase(TechBase.IS)
                .setIntroLevel(false).setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(TechRating.X, TechRating.X, TechRating.X, TechRating.E)
                .setISAdvancement(3095, 3100, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.RS)
                .setProductionFactions(Faction.RS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
}