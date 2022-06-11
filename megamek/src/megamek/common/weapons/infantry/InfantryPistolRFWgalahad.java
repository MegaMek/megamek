/**
 * MegaMek - Copyright (C) 2004,2005, 2022 MegaMekTeam
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
package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

/**
 * @since March 20, 2022
 * @author Hammer
 */
public class InfantryPistolRFWgalahad extends InfantryWeapon {
   private static final long serialVersionUID = -3164871600230559641L;

   public InfantryPistolRFWgalahad() {
       super();

       name = "Pistol (RFW Galahad)";
       setInternalName(name);
       addLookupName("RFW Galahad");
       ammoType = AmmoType.T_INFANTRY;
       bv = 0.315;
       tonnage = 0.0021;
       infantryDamage = 0.32;
       infantryRange = 1;
       ammoWeight = 0.000005;
       cost = 200;
       ammoCost = 20;
       shots = 10;
       bursts = 1;
       flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
       rulesRefs = "Shrapnel #3";
       techAdvancement
               .setTechBase(TECH_BASE_IS)
               .setTechRating(RATING_D)
               .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
               .setISAdvancement(DATE_NONE, DATE_NONE, 2100, DATE_NONE, DATE_NONE)
               .setISApproximate(false, false, true, false, false)
               .setProductionFactions(F_FS);
   }
}
