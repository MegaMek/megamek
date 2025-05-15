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
package megamek.common.weapons.flamers;

import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public class CLFlamer extends FlamerWeapon {
    private static final long serialVersionUID = 8782512971175525221L;

    public CLFlamer() {
        super();
        this.name = "Flamer";
        this.setInternalName("CLFlamer");
        this.addLookupName("Clan Flamer");
        this.heat = 3;
        this.damage = 2;
        this.infDamageClass = WeaponType.WEAPON_BURST_4D6;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.tonnage = 0.5;
        this.criticals = 1;
        this.bv = 6;
        this.cost = 7500;
        this.shortAV = 2;
        this.maxRange = RANGE_SHORT;
        this.atClass = CLASS_POINT_DEFENSE;
        rulesRefs = "218, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(TechRating.X, TechRating.C, TechRating.A, TechRating.A)
                .setClanAdvancement(2820, 2827, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.CFM)
                .setProductionFactions(Faction.CFM);
    }
}
