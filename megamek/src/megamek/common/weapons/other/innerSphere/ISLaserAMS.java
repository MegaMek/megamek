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

package megamek.common.weapons.other.innerSphere;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.weapons.lasers.LaserWeapon;

/**
 * @author Sebastian Brocks
 * @since Sep 25, 2004
 */
public class ISLaserAMS extends LaserWeapon {
    @Serial
    private static final long serialVersionUID = -7448728413011101076L;

    public ISLaserAMS() {
        super();
        name = "Laser AMS";
        setInternalName("ISLaserAntiMissileSystem");
        addLookupName("IS Laser Anti-Missile System");
        addLookupName("IS Laser AMS");
        addLookupName("ISLaserAMS");
        sortingName = "Anti-Missile System Laser";
        heat = 7;
        rackSize = 2;
        damage = 3; // for manual operation
        minimumRange = 0;
        shortRange = 1;
        mediumRange = 1;
        longRange = 1;
        extremeRange = 1;
        maxRange = RANGE_SHORT;
        shortAV = 3;
        ammoType = AmmoType.AmmoTypeEnum.NA;
        tonnage = 1.5;
        criticalSlots = 2;
        bv = 45;
        atClass = CLASS_AMS;
        // we need to remove the direct fire flag again, so TC weight is not
        // affected
        flags = flags.or(F_MEK_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON).andNot(F_PROTO_WEAPON)
              .or(F_AUTO_TARGET).or(F_AMS).or(F_ENERGY).andNot(F_DIRECT_FIRE);
        setModes(new String[] { "On", "Off" });
        setInstantModeSwitch(false);
        cost = 225000;
        rulesRefs = "134, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3059, 3079, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
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
