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


public class InfantryShotgunSGM3 extends InfantryWeapon {

   private static final long serialVersionUID = -3164871600230559641L;

   public InfantryShotgunSGM3() {
       super();

       name = "Shotgun (SGM-3)";
       setInternalName(name);
       addLookupName("SGM-3");
       ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
       bv = .248;
       tonnage =  0.0029;
       infantryDamage =  0.25;
       infantryRange =  1;
       ammoWeight =  0.0029;
       cost = 1000;
       ammoCost =  30;
       shots =  12;
       bursts =  4;
       flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
       rulesRefs = "Shrapnel #7";
       techAdvancement
       .setTechBase(TechBase.IS)
       .setTechRating(TechRating.C)
       .setAvailability(AvailabilityValue.E,AvailabilityValue.X,AvailabilityValue.X,AvailabilityValue.X)
       .setISAdvancement(DATE_NONE, DATE_NONE,2100,DATE_NONE,DATE_NONE)
       .setISApproximate(false, false, true, false, false)
       .setProductionFactions(Faction.TA);
   }
}