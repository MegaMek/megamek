/* MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.capitalweapons;

import megamek.common.AmmoType;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class AR10Weapon extends CapitalMissileWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public AR10Weapon() {
        super();
        // assume a barracuda is loaded
        this.name = "Capital Missile Launcher (AR10 Launcher)";
        this.setInternalName(this.name);
        this.addLookupName("AR10");
        this.addLookupName("CLAR10");
        this.shortName = "AR10";
        this.heat = 20; // This should reflect the maximum possible heat
        this.damage = 2;
        this.ammoType = AmmoType.T_AR10;
        this.shortRange = 20;
        this.mediumRange = 30;
        this.longRange = 40;
        this.extremeRange = 50;
        this.tonnage = 250.0;
        this.bv = 961;
        this.cost = 250000;
        this.flags = flags.or(F_MISSILE);
        // assume Barracuda is loaded
        this.shortAV = 2;
        this.medAV = 2;
        this.longAV = 2;
        this.extAV = 2;
        this.maxRange = RANGE_EXT;
        this.atClass = CLASS_AR10;
        rulesRefs = "210,TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2540, 2550, 3055, 2950, 3051)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2540, 2550, 3055, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_FS, F_LC);
    }
}
