/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2010-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;

/**
 * @author Ben Grills
 * @since Sep 25, 2004
 */
public class CapMissKrakenWeapon extends CapitalMissileWeapon {
    @Serial
    private static final long serialVersionUID = 8756042527483383101L;

    public CapMissKrakenWeapon() {
        super();
        this.name = "Capital Missile Launcher (Kraken)";
        this.setInternalName(this.name);
        this.addLookupName("Kraken");
        this.shortName = "Kraken";
        this.heat = 50;
        this.damage = 10;
        this.ammoType = AmmoType.AmmoTypeEnum.KRAKENM;
        this.shortRange = 11;
        this.mediumRange = 22;
        this.longRange = 34;
        this.extremeRange = 46;
        this.tonnage = 190.0;
        this.bv = 1914.0;
        this.cost = 455000;
        this.flags = flags.or(F_MISSILE);
        this.atClass = CLASS_CAPITAL_MISSILE;
        this.shortAV = 10;
        this.medAV = 10;
        this.longAV = 10;
        this.extAV = 10;
        this.missileArmor = 100;
        this.maxRange = RANGE_EXT;
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3053, 3057, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CS, Faction.DC)
              .setProductionFactions(Faction.DC);
    }
}
