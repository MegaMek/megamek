/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.other.clan;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.weapons.AmmoWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 25, 2004
 */
public class CLAMS extends AmmoWeapon {
    @Serial
    private static final long serialVersionUID = 7447941274169853546L;

    public CLAMS() {
        super();
        name = "Anti-Missile System";
        setInternalName("CLAntiMissileSystem");
        addLookupName("Clan Anti-Missile Sys");
        addLookupName("Clan AMS");
        addLookupName("ClanAMS");
        addLookupName("ClAMS");
        heat = 1;
        rackSize = 2;
        damage = 2; // for manual operation
        minimumRange = 0;
        shortRange = 1;
        mediumRange = 1;
        longRange = 1;
        extremeRange = 1;
        maxRange = RANGE_SHORT;
        shortAV = 3;
        ammoType = AmmoType.AmmoTypeEnum.AMS;
        tonnage = 0.5;
        criticalSlots = 1;
        bv = 32.0;
        flags = flags.or(F_AUTO_TARGET).or(F_AMS).or(F_BALLISTIC).or(F_MEK_WEAPON).or(F_AERO_WEAPON).or(F_TANK_WEAPON)
              .or(F_PROTO_WEAPON);
        setModes(new String[] { "On", "Off" });
        setInstantModeSwitch(false);
        cost = 100000;
        atClass = CLASS_AMS;
        rulesRefs = "204, TM";
        techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
              .setClanAdvancement(2824, 2831, 2835, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false).setPrototypeFactions(Faction.CSA)
              .setProductionFactions(Faction.CSA).setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        return 0;
    }

    @Override
    public boolean isAlphaStrikePointDefense() {
        return true;
    }
}
