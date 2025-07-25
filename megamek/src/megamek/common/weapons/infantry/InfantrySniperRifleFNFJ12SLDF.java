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


public class InfantrySniperRifleFNFJ12SLDF extends InfantryWeapon {

    /**
    *
    */
   private static final long serialVersionUID = -3164871600230559641L;

   public InfantrySniperRifleFNFJ12SLDF() {
       super();

       name = "Sniper Rifle (FNF-J12 (SLDF))";
       setInternalName(name);
       addLookupName("FNF-J12 (SLDF)");
       ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
       bv = .336;
       tonnage =  0.006;
       infantryDamage =  0.42;
       infantryRange =  6;
       ammoWeight =  0.006;
       cost = 3500;
       ammoCost =  70;
       shots =  8;
       bursts =  1;
       flags = flags.or(F_NO_FIRES).or(F_DIRECT_FIRE).or(F_BALLISTIC);
       rulesRefs = "Shrapnel #1";
       techAdvancement
       .setTechBase(TechBase.IS)
       .setTechRating(TechRating.E)
       .setAvailability(AvailabilityValue.E,AvailabilityValue.E,AvailabilityValue.E,AvailabilityValue.E)
       .setISAdvancement(DATE_NONE, DATE_NONE,2765,DATE_NONE,DATE_NONE)
       .setISApproximate(false, false, true, false, false)
       .setProductionFactions(Faction.TH);
   }
}