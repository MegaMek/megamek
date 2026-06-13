/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.verifier;

import java.util.EnumMap;

import jakarta.annotation.Nonnull;
import megamek.common.enums.TechRating;
import megamek.common.equipment.MiscType;
import megamek.common.units.Entity;

public class SupportVeeStructure extends Structure {

    static final EnumMap<TechRating, Double> SV_TECH_RATING_STRUCTURE_MULTIPLIER = new EnumMap<>(TechRating.class);

    static {
        SV_TECH_RATING_STRUCTURE_MULTIPLIER.put(TechRating.A, 1.60);
        SV_TECH_RATING_STRUCTURE_MULTIPLIER.put(TechRating.B, 1.30);
        SV_TECH_RATING_STRUCTURE_MULTIPLIER.put(TechRating.C, 1.15);
        SV_TECH_RATING_STRUCTURE_MULTIPLIER.put(TechRating.D, 1.00);
        SV_TECH_RATING_STRUCTURE_MULTIPLIER.put(TechRating.E, 0.85);
        SV_TECH_RATING_STRUCTURE_MULTIPLIER.put(TechRating.F, 0.66);
    }

    Entity sv;

    public SupportVeeStructure(Entity supportVee) {
        this.sv = supportVee;
    }

    public static double getWeightStructure(Entity sv) {
        double baseChassisVal = sv.getBaseChassisValue();
        double techRatingMultiplier = SV_TECH_RATING_STRUCTURE_MULTIPLIER.getOrDefault(sv.getStructuralTechRating(),
              1.0);
        double chassisModMultiplier = 1;
        if (sv.hasMisc(MiscType.F_AMPHIBIOUS)) {
            chassisModMultiplier *= 1.75;
        }
        if (sv.hasMisc(MiscType.F_ARMORED_CHASSIS)) {
            chassisModMultiplier *= 1.5;
        }
        if (sv.hasMisc(MiscType.F_BICYCLE)) {
            chassisModMultiplier *= 0.75;
        }
        if (sv.hasMisc(MiscType.F_CONVERTIBLE)) {
            chassisModMultiplier *= 1.1;
        }
        if (sv.hasMisc(MiscType.F_DUNE_BUGGY)) {
            chassisModMultiplier *= 1.5;
        }
        if (sv.hasMisc(MiscType.F_ENVIRONMENTAL_SEALING)) {
            chassisModMultiplier *= 2;
        }
        if (sv.hasMisc(MiscType.F_EXTERNAL_POWER_PICKUP)) {
            chassisModMultiplier *= 1.1;
        }
        if (sv.hasMisc(MiscType.F_HYDROFOIL)) {
            chassisModMultiplier *= 1.7;
        }
        if (sv.hasMisc(MiscType.F_MONOCYCLE)) {
            chassisModMultiplier *= 0.5;
        }
        if (sv.hasMisc(MiscType.F_OFF_ROAD)) {
            chassisModMultiplier *= 1.5;
        }
        if (sv.hasMisc(MiscType.F_PROP)) {
            chassisModMultiplier *= 1.2;
        }
        if (sv.hasMisc(MiscType.F_SNOWMOBILE)) {
            chassisModMultiplier *= 1.75;
        }
        if (sv.hasMisc(MiscType.F_STOL_CHASSIS)) {
            chassisModMultiplier *= 1.5;
        }
        if (sv.hasMisc(MiscType.F_SUBMERSIBLE)) {
            chassisModMultiplier *= 1.8;
        }
        if (sv.hasMisc(MiscType.F_TRACTOR_MODIFICATION)) {
            chassisModMultiplier *= 1.2;
        }
        if (sv.hasMisc(MiscType.F_TRAILER_MODIFICATION)) {
            chassisModMultiplier *= 0.8;
        }
        if (sv.hasMisc(MiscType.F_ULTRA_LIGHT)) {
            chassisModMultiplier *= 0.5;
        }
        if (sv.hasMisc(MiscType.F_VSTOL_CHASSIS)) {
            chassisModMultiplier *= 2;
        }

        double weight = baseChassisVal * techRatingMultiplier * chassisModMultiplier * sv.getWeight();
        Ceil roundWeight = Ceil.HALF_TON;
        if (sv.getWeight() < 5) {
            roundWeight = Ceil.KILO;
        }
        return TestEntity.floor(weight, roundWeight);
    }

    @Override
    public double getWeightStructure(double weight, @Nonnull Ceil roundWeight) {
        return getWeightStructure(sv);
    }

}
