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

package megamek.common.weapons.ppc.innerSphere;

import megamek.common.weapons.ppc.PPCWeapon;

/**
 * @author Andrew Hunter
 * @since Sep 13, 2004
 */
public class ISKinsSlaughterPPC extends PPCWeapon {
    private static final long serialVersionUID = 6733393836643781374L;

    public ISKinsSlaughterPPC() {
        super();
        this.name = "Kinslaughter H ER PPC";
        this.setInternalName("ISKinHERPPC");
        this.addLookupName("IS Kinslaughter H ER PPC");
        sortingName = "PPC ER Kins";
        this.heat = 13;
        this.damage = 10;
        this.shortRange = 4;
        this.mediumRange = 10;
        this.longRange = 16;
        this.extremeRange = 24;
        this.waterShortRange = 4;
        this.waterMediumRange = 10;
        this.waterLongRange = 16;
        this.waterExtremeRange = 24;
        this.tonnage = 7.0;
        this.criticalSlots = 3;
        this.bv = 229;
        this.cost = 450000;
        // Since this is a SL Era ER PPC variant mentioned in Spartan Fluff
        // This weapons was actually blended into IO's Enhanced PPC and should be considered non-canon
        // for IS factions
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
              .setISAdvancement(2740, 2751, DATE_NONE, 2860, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2740, 2751, DATE_NONE, 2831, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
    }
}
