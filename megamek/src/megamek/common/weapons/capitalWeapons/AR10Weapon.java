/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.equipment.Mounted;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class AR10Weapon extends CapitalMissileWeapon {
    @Serial
    private static final long serialVersionUID = 8756042527483383101L;

    public AR10Weapon() {
        super();
        // assume a barracuda is loaded
        this.name = "Capital Missile Launcher (AR10 Launcher)";
        this.setInternalName(this.name);
        this.addLookupName("AR10");
        this.addLookupName("CLAR10");
        this.shortName = "AR10";
        this.heat = 20; // This should reflect the maximum possible heat
        this.damage = 2;
        this.ammoType = AmmoType.AmmoTypeEnum.AR10;
        this.shortRange = 20;
        this.mediumRange = 30;
        this.longRange = 40;
        this.extremeRange = 50;
        this.tonnage = 250.0;
        this.bv = 961.0;
        this.cost = 250000;
        this.flags = flags.or(F_MISSILE);
        // assume Barracuda is loaded
        this.shortAV = 2;
        this.medAV = 2;
        this.longAV = 2;
        this.extAV = 2;
        this.maxRange = RANGE_EXT;
        this.atClass = CLASS_AR10;
        rulesRefs = "210, TM";
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2540, 2550, 3055, 2950, 3051)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2540, 2550, 3055, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS, Faction.LC);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> linked) {
        return 3;
    }
}
