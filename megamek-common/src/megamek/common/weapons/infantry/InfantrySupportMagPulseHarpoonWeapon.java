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
import megamek.common.TechConstants;

/**
 * @author Ben Grills
 */
public class InfantrySupportMagPulseHarpoonWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantrySupportMagPulseHarpoonWeapon() {
        super();
        techLevel.put(3071,TechConstants.T_IS_TW_NON_BOX);
        name = "Magpulse Harpoon Gun";
        setInternalName(name);
        addLookupName("InfantryMagpulseHarpoonGun");
        addLookupName("MagpulseHarpoonGun");
        ammoType = AmmoType.T_NA;
        cost = 75;
        bv = 1.47;
        flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_MISSILE).or(F_INF_SUPPORT);
        infantryDamage = 0.37;
        infantryRange = 3;
        crew = 2;
        introDate = 3079;
        techLevel.put(3079,techLevel.get(3071));
        availRating = new int[]{RATING_X,RATING_X,RATING_F};
        techRating = RATING_E;
    }
}

//TODO
/**
Any vehicular unit (including battle armor, ProtoMechs, Combat Vehicles and BattleMechs)
successfully struck by a shot from a MagPulse harpoon gun will suffer electronic interference
sufficient enough to cause a -1 roll modifier for all Gunnery and Sensor Operations Skill Checks 
by its pilot for 10 seconds (1 Total Warfare combat turn), in addition to any physical 
damage the weapon delivers. These effects are notcumulative, and are not enhanced
by multiple harpoon hits at the same time.
*/