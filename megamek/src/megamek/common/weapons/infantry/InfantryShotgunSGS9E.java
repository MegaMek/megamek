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


public class InfantryShotgunSGS9E extends InfantryWeapon {

   private static final long serialVersionUID = -3164871600230559641L;

   public InfantryShotgunSGS9E() {
       super();

       name = "Shotgun (SGS-9E)";
       setInternalName(name);
       addLookupName("SGS-9E");
       ammoType = AmmoType.T_INFANTRY;
       bv = 1.375;
       tonnage =  0.0045;
       infantryDamage =  0.64;
       infantryRange =  1;
       ammoWeight =  0.0045;
       cost = 1500;
       ammoCost =  50;
       shots =  21;
       bursts =  3;
       flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_INF_ENCUMBER);
       rulesRefs = "Shrapnel #7";
       techAdvancement
       .setTechBase(TECH_BASE_CLAN)
       .setTechRating(RATING_C)
       .setAvailability(RATING_X,RATING_E,RATING_E,RATING_E)
       .setClanAdvancement(DATE_NONE, DATE_NONE,2850,DATE_NONE,DATE_NONE)
       .setClanApproximate(false, false, true, false, false)
       .setProductionFactions(F_CLAN);
   }
}