/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @author Ben Grills
 */
public class InfantryPistolClanPulseLaserPistolWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryPistolClanPulseLaserPistolWeapon() {
        super();

        name = "Pulse Laser Pistol [Clan]";
        setInternalName(name);
        addLookupName("InfantryClanPulseLaserPistol");
        addLookupName("Clan Pulse Laser Pistol");
        ammoType = AmmoType.T_INFANTRY;
        cost = 1500;
        bv = 0.61;
        tonnage = .001;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_ENERGY);
        infantryDamage = 0.20;
        infantryRange = 1;
        ammoWeight = 0.0003;
        shots = 10;
        bursts = 2;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(2835, 2840, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CFM)
                .setProductionFactions(F_CFM).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_C, RATING_C, RATING_C);

    }
}
