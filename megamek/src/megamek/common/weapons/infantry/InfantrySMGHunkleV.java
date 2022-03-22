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
 * Created on March 20, 2022
 * @author Hammer
 */


package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

public class InfantrySMGHunkleV extends InfantryWeapon {

   private static final long serialVersionUID = -3164871600230559641L;

   public InfantrySMGHunkleV() {
       super();

       name = "SMG (Hunkle V)";
       setInternalName(name);
       addLookupName("Hunkle V");
       ammoType = AmmoType.T_INFANTRY;
       bv = 1;
       tonnage =  0.0027;
       infantryDamage =  0.55;
       infantryRange =  1;
       ammoWeight =  0.0027;
       cost = 600;
       ammoCost =  90;
       shots =  100;
       bursts =  10;
       flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
       rulesRefs = "Shrapnel #5";
       techAdvancement
       .setTechBase(TECH_BASE_IS)
       .setTechRating(RATING_C)
       .setAvailability(RATING_X,RATING_B,RATING_B,RATING_B)
       .setISAdvancement(DATE_NONE, DATE_NONE,2335,DATE_NONE,DATE_NONE)
       .setISApproximate(false, false, true, false, false)
       .setProductionFactions(F_TC);


   }
}