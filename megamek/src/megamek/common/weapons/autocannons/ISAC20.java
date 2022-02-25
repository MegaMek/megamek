/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons.autocannons;

/**
 * @author Andrew Hunter
 * @since Sep 25, 2004
 */
public class ISAC20 extends ACWeapon {
    private static final long serialVersionUID = 4780847244648362671L;

    public ISAC20() {
        super();
        name = "AC/20";
        setInternalName("Autocannon/20");
        addLookupName("IS Auto Cannon/20");
        addLookupName("Auto Cannon/20");
        addLookupName("AutoCannon/20");
        addLookupName("ISAC20");
        addLookupName("IS Autocannon/20");
        heat = 7;
        damage = 20;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 14.0;
        criticals = 10;
        bv = 178;
        cost = 300000;
        shortAV = 20;
        maxRange = RANGE_SHORT;
        explosionDamage = damage;
        rulesRefs = "208, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setTechRating(RATING_C)
                .setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(2488, 2500, 2502, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2488, 2500, 2502, 2850, DATE_NONE)
                .setClanApproximate(false, false, false, true, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
    }
}
