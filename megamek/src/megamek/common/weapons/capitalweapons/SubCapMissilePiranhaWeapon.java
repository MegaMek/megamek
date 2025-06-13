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
package megamek.common.weapons.capitalweapons;

import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.alphaStrike.AlphaStrikeElement;

import java.io.Serial;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class SubCapMissilePiranhaWeapon extends SubCapMissileWeapon {
    @Serial
    private static final long serialVersionUID = 3827228773282489872L;

    public SubCapMissilePiranhaWeapon() {
        super();
        name = "Sub-Capital Missile Launcher (Piranha)";
        setInternalName(name);
        addLookupName("Piranha");
        shortName = "Piranha";
        heat = 9;
        damage = 3;
        ammoType = AmmoType.AmmoTypeEnum.PIRANHA;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 100.0;
        bv = 589;
        cost = 75000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        missileArmor = 30;
        maxRange = RANGE_LONG;
        flags = flags.or(F_AERO_WEAPON).or(F_MISSILE);
        atClass = CLASS_CAPITAL_MISSILE;
        rulesRefs = "156, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
                .setISAdvancement(DATE_NONE, 3060, 3072, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, 3070, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(Faction.WB)
                .setProductionFactions(Faction.WB)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> linked) {
        return (range <= AlphaStrikeElement.LONG_RANGE) ? 3 : 0;
    }
}
