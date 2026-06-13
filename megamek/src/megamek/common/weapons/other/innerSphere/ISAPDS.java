/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2015-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.other.innerSphere;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.AmmoWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 25, 2004
 */
public class ISAPDS extends AmmoWeapon {
    @Serial
    private static final long serialVersionUID = 5678281956614161074L;

    public ISAPDS() {
        super();
        name = "RISC Advanced Point Defense System";
        shortName = "RISC APDS";
        setInternalName("ISAPDS");
        heat = 2;
        rackSize = 2;
        damage = 2; // for manual operation
        ammoType = AmmoType.AmmoTypeEnum.APDS;
        tonnage = 3;
        criticalSlots = 2;
        minimumRange = 0;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 3;
        maxRange = RANGE_LONG;
        bv = 64;
        flags = flags.or(F_MEK_WEAPON).or(F_TANK_WEAPON)
              .or(F_AUTO_TARGET).or(F_AMS).or(F_BALLISTIC);
        setModes(new String[] { "On", "Off" });
        setInstantModeSwitch(false);
        cost = 200000;
        rulesRefs = "85, IO:AE";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E)
              .setISAdvancement(3134, 3137, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.RS)
              .setProductionFactions(Faction.RS);
    }

    @Override
    public boolean isAlphaStrikePointDefense() {
        return true;
    }

    @Override
    public double getBattleForceDamage(int range) {
        return 0;
    }
}
