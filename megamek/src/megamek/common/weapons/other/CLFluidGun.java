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
package megamek.common.weapons.other;

/**
 * @author beerockxs
 */
public class CLFluidGun extends FluidGunWeapon {
    private static final long serialVersionUID = 5043640099544278749L;

    public CLFluidGun() {
        super();

        name = "Fluid Gun";
        setInternalName("Clan Fluid Gun");
        addLookupName("CLFluidGun");
        rackSize = 1;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        bv = 6;
        heat = 0;
        criticals = 2;
        svslots = 1;
        tonnage = 2;
        cost = 35000;
        rulesRefs = "313, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        // December 2021 - CGL requested we move this to Advanced.
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_B)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
    }
}
