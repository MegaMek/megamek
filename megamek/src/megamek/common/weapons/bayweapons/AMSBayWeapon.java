/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons.bayweapons;

import megamek.common.TechAdvancement;
import megamek.common.TechConstants;

/**
 * @author Magnus Kerensky
 */
public class AMSBayWeapon extends AmmoBayWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527493383101L;

    /**  According to the rules in StratOps p96, point defense bays containing
     * only AMS type weapons behave a bit differently, so we need to separate them.
     * 
     */
    public AMSBayWeapon() {
        super();
        // tech levels are a little tricky
        this.name = "AMS Bay";
        this.setInternalName(this.name);
        this.heat = 0;
        this.damage = DAMAGE_VARIABLE;
        this.shortRange = 1;
        this.mediumRange = 0;
        this.longRange = 0;
        this.extremeRange = 0;
        this.tonnage = 0.0f;
        this.bv = 0;
        this.cost = 0;
        flags = flags.or(F_AUTO_TARGET).or(F_AMS).or(F_AERO_WEAPON);
        setModes(new String[] { "On", "Off" });
        setInstantModeSwitch(false);
        this.atClass = CLASS_AMS;
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_ALL);
        techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, 3071);
        techAdvancement.setTechRating(RATING_C);
        techAdvancement.setAvailability( new int[] { RATING_E, RATING_E, RATING_E, RATING_E });
    }
}
