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

public class InfantrySMGSpartacus extends InfantryWeapon {

   private static final long serialVersionUID = -3164871600230559641L;

   public InfantrySMGSpartacus() {
       super();

       name = "SMG (Spartacus)";
       setInternalName(name);
       addLookupName("SMG (Spartacus)");
       ammoType = AmmoType.T_INFANTRY;
       bv = .44;
       tonnage =  0.0028;
       infantryDamage =  0.41;
       infantryRange =  2;
       ammoWeight =  0.0028;
       cost = 750;
       ammoCost =  50;
       shots =  50;
       bursts =  8;
       flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
       rulesRefs = "Shrapnel #5";
       techAdvancement
       .setTechBase(TECH_BASE_IS)
       .setTechRating(RATING_D)
       .setAvailability(RATING_X,RATING_X,RATING_X,RATING_D)
       .setISAdvancement(DATE_NONE, DATE_NONE,2920,DATE_NONE,DATE_NONE)
       .setISApproximate(false, false, true, false, false)
       .setProductionFactions(F_MH);


   }
}