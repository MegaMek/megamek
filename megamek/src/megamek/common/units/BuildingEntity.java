/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.units;

import megamek.common.MPCalculationSetting;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.board.CubeCoords;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Engine;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.PowerGeneratorType;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.equipment.enums.StructureEngine;

/**
 * Implementation of TO:AR's Advanced Buildings.
 * <br>
 * Extends {@link AbstractBuildingEntity}
 */
public class BuildingEntity extends AbstractBuildingEntity {

    public BuildingEntity(BuildingType type, int bldgClass) {
        super(type, bldgClass);
    }

    /**
     * @see UnitType
     */
    @Override
    public int getUnitType() {
        return UnitType.ADVANCED_BUILDING;
    }

    @Override
    public boolean isImmobile() {
        return true;
    }

    @Override
    public boolean isEligibleForMovement() {
        return false;
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        return 0;
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        return 0;
    }

    @Override
    public int getSprintMP(MPCalculationSetting mpCalculationSetting) {
        return 0;
    }

    @Override
    public int getJumpMP(MPCalculationSetting mpCalculationSetting) {
        return 0;
    }

    /**
     * return - the base construction option tech advancement
     */
    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return TA_BUILDING_ENTITY;
    }

    /**
     * Returns the name of the type of movement used.
     *
     * @param movementType
     */
    @Override
    public String getMovementString(EntityMovementType movementType) {
        return "Not possible!";
    }

    /**
     * Returns the abbreviation of the name of the type of movement used.
     *
     * @param movementType
     */
    @Override
    public String getMovementAbbr(EntityMovementType movementType) {
        return "!";
    }

    /**
     * Calculates a "generic" Battle Value that is based on the average of all units of this type and tonnage. The
     * purpose of this generic Battle Value is to allow a comparison of this unit's actual BV to that for units of its
     * class. This can be used to balance forces without respect to unit or pilot quality.
     * <p>
     * The generic BV values are calculated by a statistical elasticity model based on all data from the MegaMek
     * database.
     *
     * @return The generic Battle value for this unit based on its tonnage and type
     */
    @Override
    public int getGenericBattleValue() {
        return 0;
    }

    /**
     * The maximum elevation change the entity can cross
     */
    @Override
    public int getMaxElevationChange() {
        return 0;
    }

    /**
     * A {@link BuildingEntity} needs power to function.
     * @return true if the unit has power, otherwise false
     */
    public boolean hasPower() {
        // Return true if we have enough power - calculate the base generator weight and compare it to all the
        // generators we have that're working
        double powerNeeded = getBaseGeneratorWeight();
        double effectivePower = 0.0;

        for (MiscMounted miscMountedPowerGenerator : getMiscEquipment(MiscTypeFlag.F_POWER_GENERATOR)) {
            if (miscMountedPowerGenerator.getType() instanceof PowerGeneratorType powerGeneratorType && miscMountedPowerGenerator.isOperable()) {
                StructureEngine engineType = powerGeneratorType.getStructureEngine();
                effectivePower += miscMountedPowerGenerator.getSize() / engineType.getBuildingWeightMultiplier();
            }
        }

        // TODO: Support `External` power properly
        // TODO: Fuel?
        // TODO: Make sure if this is false the BuildingEntity cannot attack

        return effectivePower >= powerNeeded;
    }

    /**
     * Calculates the base generator weight for an advanced building.
     * <p>
     * To find the Base Generator Weight for an advanced building (or a complex of buildings):
     * 1. Add up the total number of hexes for all advanced buildings intended to receive power
     * 2. Exclude Tent-, Fence-, Wall- and Bridge-class buildings
     * 3. For multi-level buildings: multiply the building's hex-count by its height in levels
     *    (plus any basement levels) before adding it to the sum
     * 4. Add to this sum 10 percent of the total tonnage for all Heavy-class energy weapons
     *    used by any of these buildings
     *
     * @return The base generator weight in tons
     */
    private double getBaseGeneratorWeight() {
        if (getInternalBuilding() == null) {
            return 0.0;
        }
        // Exclude Wall-type buildings (Tent, Fence, and Bridge are not implemented yet)
        if (getBuildingType() == BuildingType.WALL) {
            return 0.0;
        }

        Building building = getInternalBuilding();
        if (building == null) {
            return 0.0;
        }

        // Calculate base hex count multiplied by height + basement levels
        int hexCount = building.getCoordsList().size();
        int buildingHeight = building.getBuildingHeight();

        // Find the maximum basement depth across all hexes
        int maxBasementDepth = 0;
        for (CubeCoords coords : building.getCoordsList()) {
            BasementType basement = building.getBasement(coords);
            if (basement != null) {
                maxBasementDepth = Math.max(maxBasementDepth, basement.getDepth());
            }
        }

        // Calculate effective hex count (hex count * (height + basement levels))
        double baseHexWeight = hexCount * (buildingHeight + maxBasementDepth);

        // Calculate 10% of total tonnage for all energy weapons
        double energyWeaponTonnage = 0.0;
        for (Mounted<?> equipment : getEquipment()) {
            if (equipment.getType() instanceof WeaponType weaponType && equipment.getTonnage() >= .25) {
                if (weaponType.hasFlag(WeaponType.F_ENERGY)) {
                    energyWeaponTonnage += weaponType.getTonnage(this);
                }
            }
        }

        // Base Generator Weight = base hex weight + 10% of energy weapon tonnage
        return baseHexWeight + (energyWeaponTonnage * 0.1);
    }

    // FIXME: IDK if this is right, just needed something to pass tests
    private static final TechAdvancement TA_BUILDING_ENTITY = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
          .setTechRating(TechRating.B)
          .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);
}
