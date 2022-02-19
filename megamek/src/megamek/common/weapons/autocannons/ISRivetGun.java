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

import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class ISRivetGun extends NailRivetGunWeapon {
    private static final long serialVersionUID = -5198228513368748633L;

    public ISRivetGun() {
        super();
        name = "Rivet Gun";
        setInternalName(name);
        addLookupName("ISRivet Gun");
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 2310);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability(RATING_C, RATING_C, RATING_C, RATING_X);
    }
}
