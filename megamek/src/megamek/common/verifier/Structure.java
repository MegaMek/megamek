/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.equipment.EquipmentType;
import megamek.common.units.EntityMovementMode;

public class Structure {

    private int structureType;
    private boolean isSuperHeavy;
    private EntityMovementMode movementMode;

    public Structure() {
    }

    public Structure(int structureType, boolean superHeavy,
          EntityMovementMode movementMode) {
        this.structureType = structureType;
        isSuperHeavy = superHeavy;
        this.movementMode = movementMode;
    }

    public double getWeightStructure(double weight, Ceil roundWeight) {
        return Structure.getWeightStructure(structureType, weight, roundWeight,
              isSuperHeavy, movementMode);
    }

    public static double getWeightStructure(int structureType, double weight, Ceil roundWeight,
          boolean isSuperHeavy, EntityMovementMode movementMode) {
        double multiplier = 1.0;
        if (movementMode == EntityMovementMode.TRIPOD) {
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
              && ((movementMode != EntityMovementMode.NAVAL)
              && (movementMode != EntityMovementMode.SUBMARINE))) {
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
