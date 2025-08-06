/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.verifier;

import java.util.EnumMap;

import megamek.common.Entity;
import megamek.common.ITechnology;
import megamek.common.MiscType;

public class SupportVeeStructure extends Structure {

    static final EnumMap<ITechnology.TechRating, Double> SV_TECH_RATING_STRUCTURE_MULTIPLIER = new EnumMap<>(ITechnology.TechRating.class);

    static {
        SV_TECH_RATING_STRUCTURE_MULTIPLIER.put(ITechnology.TechRating.A, 1.60);
        SV_TECH_RATING_STRUCTURE_MULTIPLIER.put(ITechnology.TechRating.B, 1.30);
        SV_TECH_RATING_STRUCTURE_MULTIPLIER.put(ITechnology.TechRating.C, 1.15);
        SV_TECH_RATING_STRUCTURE_MULTIPLIER.put(ITechnology.TechRating.D, 1.00);
        SV_TECH_RATING_STRUCTURE_MULTIPLIER.put(ITechnology.TechRating.E, 0.85);
        SV_TECH_RATING_STRUCTURE_MULTIPLIER.put(ITechnology.TechRating.F, 0.66);
    }

    Entity sv;

    public SupportVeeStructure(Entity supportVee) {
        this.sv = supportVee;
    }

    public static double getWeightStructure(Entity sv) {
        double baseChassisVal = sv.getBaseChassisValue();
        double trMult = SV_TECH_RATING_STRUCTURE_MULTIPLIER.getOrDefault(sv.getStructuralTechRating(), 1.0);
        double chassisModMult = 1;
        if (sv.hasMisc(MiscType.F_AMPHIBIOUS)) {
            chassisModMult *= 1.75;
        }
        if (sv.hasMisc(MiscType.F_ARMORED_CHASSIS)) {
            chassisModMult *= 1.5;
        }
        if (sv.hasMisc(MiscType.F_BICYCLE)) {
            chassisModMult *= 0.75;
        }
        if (sv.hasMisc(MiscType.F_CONVERTIBLE)) {
            chassisModMult *= 1.1;
        }
        if (sv.hasMisc(MiscType.F_DUNE_BUGGY)) {
            chassisModMult *= 1.5;
        }
        if (sv.hasMisc(MiscType.F_ENVIRONMENTAL_SEALING)) {
            chassisModMult *= 2;
        }
        if (sv.hasMisc(MiscType.F_EXTERNAL_POWER_PICKUP)) {
            chassisModMult *= 1.1;
        }
        if (sv.hasMisc(MiscType.F_HYDROFOIL)) {
            chassisModMult *= 1.7;
        }
        if (sv.hasMisc(MiscType.F_MONOCYCLE)) {
            chassisModMult *= 0.5;
        }
        if (sv.hasMisc(MiscType.F_OFF_ROAD)) {
            chassisModMult *= 1.5;
        }
        if (sv.hasMisc(MiscType.F_PROP)) {
            chassisModMult *= 1.2;
        }
        if (sv.hasMisc(MiscType.F_SNOWMOBILE)) {
            chassisModMult *= 1.75;
        }
        if (sv.hasMisc(MiscType.F_STOL_CHASSIS)) {
            chassisModMult *= 1.5;
        }
        if (sv.hasMisc(MiscType.F_SUBMERSIBLE)) {
            chassisModMult *= 1.8;
        }
        if (sv.hasMisc(MiscType.F_TRACTOR_MODIFICATION)) {
            chassisModMult *= 1.2;
        }
        if (sv.hasMisc(MiscType.F_TRAILER_MODIFICATION)) {
            chassisModMult *= 0.8;
        }
        if (sv.hasMisc(MiscType.F_ULTRA_LIGHT)) {
            chassisModMult *= 0.5;
        }
        if (sv.hasMisc(MiscType.F_VSTOL_CHASSIS)) {
            chassisModMult *= 2;
        }

        double weight = baseChassisVal * trMult * chassisModMult * sv.getWeight();
        TestEntity.Ceil roundWeight = TestEntity.Ceil.HALFTON;
        if (sv.getWeight() < 5) {
            roundWeight = TestEntity.Ceil.KILO;
        }
        return TestEntity.floor(weight, roundWeight);
    }

    @Override
    public double getWeightStructure(double weight, TestEntity.Ceil roundWeight) {
        return getWeightStructure(sv);
    }

}
