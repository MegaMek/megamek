/*
* MegaMek - Copyright (C) 2023 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common.verifier;

import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;

public class Structure {

    private int structureType;
    private boolean isSuperHeavy;
    private EntityMovementMode movementmode;

    public Structure() {
    }

    public Structure(int structureType, boolean superHeavy,
            EntityMovementMode movementMode) {
        this.structureType = structureType;
        isSuperHeavy = superHeavy;
        movementmode = movementMode;
    }

    public double getWeightStructure(double weight, TestEntity.Ceil roundWeight) {
        return Structure.getWeightStructure(structureType, weight, roundWeight,
                isSuperHeavy, movementmode);
    }

    public static double getWeightStructure(int structureType, double weight,
            TestEntity.Ceil roundWeight, boolean isSuperHeavy,
            EntityMovementMode movementmode) {
        double multiplier = 1.0;
        if (movementmode == EntityMovementMode.TRIPOD) {
            multiplier = 1.1;
        }
        if (structureType == EquipmentType.T_STRUCTURE_ENDO_STEEL) {
            if (isSuperHeavy) {
                return TestEntity.ceilMaxHalf((weight / 10.0f) * multiplier,
                        roundWeight);
            } else {
                return TestEntity.ceilMaxHalf((weight / 20.0f) * multiplier,
                        roundWeight);
            }
        } else if (structureType == EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE) {
            return TestEntity.ceilMaxHalf((weight / 20.0f) * multiplier,
                    roundWeight);
        } else if (structureType == EquipmentType.T_STRUCTURE_REINFORCED) {
            return TestEntity.ceilMaxHalf((weight / 5.0f) * multiplier,
                    roundWeight);
        } else if (structureType == EquipmentType.T_STRUCTURE_COMPOSITE) {
            return TestEntity.ceilMaxHalf((weight / 20.0f) * multiplier,
                    roundWeight);
        } else if (structureType == EquipmentType.T_STRUCTURE_INDUSTRIAL) {
            if (isSuperHeavy) {
                return TestEntity.ceilMaxHalf((weight / 2.5f) * multiplier,
                        roundWeight);
            } else {
                return TestEntity.ceilMaxHalf((weight / 5.0f) * multiplier,
                        roundWeight);
            }

        } else if (structureType == EquipmentType.T_STRUCTURE_ENDO_COMPOSITE) {
            if (isSuperHeavy) {
                return TestEntity.ceilMaxHalf((weight / 10.0f) * 1.5f
                        * multiplier, roundWeight);
            } else {
                return TestEntity.ceilMaxHalf((weight / 10.0f) * 0.75f
                        * multiplier, roundWeight);
            }
        }
        if (isSuperHeavy
                && ((movementmode != EntityMovementMode.NAVAL)
                        && (movementmode != EntityMovementMode.SUBMARINE))) {
            return TestEntity.ceilMaxHalf((weight / 5.0f) * multiplier,
                    roundWeight);
        } else {
            return TestEntity.ceilMaxHalf((weight / 10.0f) * multiplier,
                    roundWeight);
        }
    }

    public String getShortName() {
        return "(" + EquipmentType.getStructureTypeName(structureType) + ")";
    }

} // End class Structure
