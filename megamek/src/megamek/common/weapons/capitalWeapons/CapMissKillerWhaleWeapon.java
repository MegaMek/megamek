/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.capitalWeapons;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class CapMissKillerWhaleWeapon extends CapitalMissileWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public CapMissKillerWhaleWeapon() {
        super();
        this.name = "Capital Missile Launcher (Killer Whale)";
        this.setInternalName(this.name);
        this.addLookupName("KillerWhale");
        this.shortName = "Killer Whale";
        this.heat = 20;
        this.damage = 4;
        this.ammoType = AmmoType.AmmoTypeEnum.KILLER_WHALE;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 150.0;
        this.bv = 769.0;
        this.cost = 150000;
        this.flags = flags.or(F_MISSILE);
        this.atClass = CLASS_CAPITAL_MISSILE;
        this.shortAV = 4;
        this.medAV = 4;
        this.longAV = 4;
        this.extAV = 4;
        this.missileArmor = 40;
        this.maxRange = RANGE_EXT;
        rulesRefs = "210, TM";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2200, 2305, 3055, 2950, 3051)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2200, 2305, 3055, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA)
              .setReintroductionFactions(Faction.FS, Faction.LC);
    }
}
