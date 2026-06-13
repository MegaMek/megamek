/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.autoCannons.clan;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.weapons.autoCannons.UACWeapon;

/**
 * @author Andrew Hunter
 * @since Oct 2, 2004
 */
public class CLUAC10 extends UACWeapon {
    @Serial
    private static final long serialVersionUID = 6077697413308875802L;

    public CLUAC10() {
        super();

        this.name = "Ultra AC/10";
        this.setInternalName("CLUltraAC10");
        this.addLookupName("Clan Ultra AC/10");
        this.heat = 3;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 10.0;
        this.criticalSlots = 4;
        this.bv = 210.0;
        this.cost = 320000;
        this.shortAV = 15;
        this.medAV = 15;
        this.maxRange = RANGE_MED;
        this.explosionDamage = damage;
        rulesRefs = "208, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.C)
              .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.CLAN)
              .setProductionFactions(Faction.CLAN);
    }
}
