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
public class InfantryPistolSerrek7994 extends InfantryWeapon {
   private static final long serialVersionUID = -3164871600230559641L;

   public InfantryPistolSerrek7994() {
       super();

       name = "Pistol (Serrek 7994)";
       setInternalName(name);
       addLookupName("Serrek 7994");
       ammoType = AmmoType.T_INFANTRY;
       bv = 0.202;
       tonnage = 0.0009;
       infantryDamage = 0.2;
       infantryRange = 1;
       ammoWeight = 0.000004;
       cost = 300;
       ammoCost = 15;
       shots = 20;
       bursts = 3;
       flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
       rulesRefs = "Shrapnel #3";
       techAdvancement
               .setTechBase(TECH_BASE_IS)
               .setTechRating(RATING_C)
               .setAvailability(RATING_X, RATING_C, RATING_B, RATING_B)
               .setISAdvancement(DATE_NONE, DATE_NONE, 2800, DATE_NONE, DATE_NONE)
               .setISApproximate(false, false, true, false, false)
               .setProductionFactions(F_FS);
   }
}
