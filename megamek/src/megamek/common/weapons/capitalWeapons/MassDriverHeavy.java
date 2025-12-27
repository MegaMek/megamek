/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class MassDriverHeavy extends MassDriverWeapon {
    @Serial
    private static final long serialVersionUID = 8756042527483383101L;

    public MassDriverHeavy() {
        super();
        this.name = "Mass Driver (Heavy)";
        this.setInternalName(this.name);
        this.addLookupName("HeavyMassDriver");
        this.shortName = "Heavy Mass Driver";
        this.heat = 90;
        this.damage = 140;
        this.ammoType = AmmoType.AmmoTypeEnum.HMASS;
        this.shortRange = 12;
        this.mediumRange = 24;
        this.longRange = 40;
        this.tonnage = 100000;
        this.bv = 16464.0;
        this.cost = 500000000;
        this.shortAV = 140;
        this.medAV = 140;
        this.longAV = 140;
        rulesRefs = "135, TO:AUE";
        techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
              .setISAdvancement(2715, DATE_NONE, DATE_NONE, 2855, 3066)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2715, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, true, false)
              .setPrototypeFactions(Faction.TH).setReintroductionFactions(Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> linked) {
        return (range <= AlphaStrikeElement.LONG_RANGE) ? 126 : 0;
    }
}
