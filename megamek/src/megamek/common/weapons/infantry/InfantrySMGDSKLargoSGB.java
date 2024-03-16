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
 /*
 * Created on March 20, 2022
 * @author Hammer
 */

package megamek.common.weapons.infantry;

import megamek.common.AmmoType;

public class InfantrySMGDSKLargoSGB extends InfantryWeapon {

    /**
    *
    */
   private static final long serialVersionUID = -3164871600230559641L;

   public InfantrySMGDSKLargoSGB() {
       super();

       name = "SMG (DSK Largo SGB)";
       setInternalName(name);
       addLookupName("DSK Largo SGB");
       ammoType = AmmoType.T_INFANTRY;
       bv = .44;
       tonnage =  0.0035;
       infantryDamage =  0.44;
       infantryRange =  2;
       ammoWeight =  0.0035;
       cost = 700;
       ammoCost =  85;
       shots =  50;
       bursts =  10;
       flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
       rulesRefs = "Shrapnel #5";
       techAdvancement
       .setTechBase(TECH_BASE_IS)
       .setTechRating(RATING_C)
       .setAvailability(RATING_X,RATING_X,RATING_D,RATING_D)
       .setISAdvancement(DATE_NONE, DATE_NONE,3050,DATE_NONE,DATE_NONE)
       .setISApproximate(false, false, true, false, false)
       .setProductionFactions(F_LC);


   }
}