/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.autocannons;

import megamek.common.EquipmentTypeLookup;
/**
 * @author Sebastian Brocks
 */
public class ISNailandRivetGun extends NailRivetGunWeapon {
    private static final long serialVersionUID = -5198228513368748633L;

    public ISNailandRivetGun() {
        super();
        name = "Nail/Rivet Gun";
        setInternalName(EquipmentTypeLookup.NAIL_RIVET_GUN);
        addLookupName("ISNailRivet Gun");
        addLookupName("ISNail Gun");
        addLookupName("Nail/Rivet Gun");
        addLookupName("CLNailRivet Gun");
        addLookupName("CLNail/Rivet Gun");
        addLookupName("CLNail Gun");
        addLookupName("ISRivet Gun");
        addLookupName("CLRivet Gun");
        addLookupName("CLNailRivetGun");
        addLookupName("Nail Gun");
        rulesRefs = "246, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2309, 2310, 2312, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, false, false, false)
                .setClanAdvancement(2309, 2310, 2312, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false)
                .setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);
    }
}

