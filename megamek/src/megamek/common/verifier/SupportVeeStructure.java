/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.verifier;

import megamek.common.Entity;
import megamek.common.MiscType;

public class SupportVeeStructure extends Structure {

    static final double[] SV_TECH_RATING_STRUCTURE_MULTIPLIER = 
        { 1.60, 1.30, 1.15, 1.00, 0.85, 0.66 };
    
    Entity sv;
    
    public SupportVeeStructure(Entity supportVee) {
        this.sv = supportVee;
    }
    
    public static double getWeightStructure(Entity sv) {
        double baseChassisVal = sv.getBaseChassisValue();
        double trMult = SV_TECH_RATING_STRUCTURE_MULTIPLIER[sv.getStructuralTechRating()];
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