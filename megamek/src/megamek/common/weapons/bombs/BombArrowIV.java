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
package megamek.common.weapons.bombs;

import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.BombType.BombTypeEnum;
import megamek.common.weapons.artillery.ArtilleryWeapon;

/**
 * @author Jay Lawson
 * @since Oct 20, 2004
 */
public class BombArrowIV extends ArtilleryWeapon {
    private static final long serialVersionUID = -1321502140176775035L;

    public BombArrowIV() {
        super();
        this.name = "Arrow IV Bomb Mount";
        this.setInternalName(BombTypeEnum.ARROW.getWeaponName());
        this.heat = 0;
        this.rackSize = 20;
        this.ammoType = AmmoType.AmmoTypeEnum.ARROW_IV_BOMB;
        this.shortRange = 1; //
        this.mediumRange = 2;
        this.longRange = 9;
        this.extremeRange = 9; // No extreme range.
        this.tonnage = 0;
        this.criticals = 0;
        this.hittable = false;
        this.bv = 0;
        this.cost = 0;
        flags = flags.or(F_BOMB_WEAPON);
        rulesRefs = "359, TO";
        techAdvancement.setTechBase(TechBase.ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(2622, 2623, DATE_NONE, 2850, 3047)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2622, 2623, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.CC);
    }
}
