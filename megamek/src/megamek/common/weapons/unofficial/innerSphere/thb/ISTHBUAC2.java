/*
  Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.unofficial.innerSphere.thb;

import megamek.common.equipment.AmmoType;
import megamek.common.weapons.autocannons.UACWeapon;

/**
 * @author Andrew Hunter
 * @since Oct 1, 2004
 */
public class ISTHBUAC2 extends UACWeapon {
    private static final long serialVersionUID = 8027434391024117813L;

    public ISTHBUAC2() {
        super();
        this.name = "Ultra AC/2 (THB)";
        this.setInternalName("ISUltraAC2 (THB)");
        this.addLookupName("IS Ultra AC/2 (THB)");
        this.heat = 1;
        this.damage = 2;
        this.rackSize = 2;
        this.ammoType = AmmoType.AmmoTypeEnum.AC_ULTRA_THB;
        this.minimumRange = 3;
        this.shortRange = 9;
        this.mediumRange = 20;
        this.longRange = 32;
        this.extremeRange = 40;
        this.tonnage = 7.0;
        this.criticals = 3;
        this.bv = 67;
        this.cost = 150000;
        // Since this are the Tactical Handbook Weapons I'm using the TM Stats.
        rulesRefs = "THB (Unofficial)";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
    }
}
