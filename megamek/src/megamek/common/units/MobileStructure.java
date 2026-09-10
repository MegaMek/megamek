/*
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

package megamek.common.units;

import megamek.common.TechAdvancement;
import megamek.common.SimpleTechLevel;
import megamek.common.MPCalculationSetting;
import megamek.common.board.CubeCoords;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.BuildingType;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Engine;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.enums.StructureEngine;

/**
 * TO:AUE Mobile Structures. Construction height excludes a ground structure's two-level undercarriage.
 * <br>
 * Extends {@link AbstractBuildingEntity}
 */
public class MobileStructure extends AbstractBuildingEntity {
    private StructureEngine powerSystem = StructureEngine.FUSION;
    private int maximumMPQuarters = 4;
    private double operatingRange;
    private int mobileCrewHits;

    public MobileStructure(BuildingType type, int bldgClass) {
        super(type, bldgClass);
        setMovementMode(EntityMovementMode.TRACKED);
        setPowerSystem(StructureEngine.FUSION);
    }

    /**
     * @see UnitType
     */
    @Override
    public int getUnitType() {
        return UnitType.MOBILE_STRUCTURE;
    }

    /**
     * return - the base construction option tech advancement
     */
    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return new TechAdvancement(TechBase.ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    /**
     * Returns the name of the type of movement used.
     *
     */
    @Override
    public String getMovementString(EntityMovementType movementType) {
        return movementType == EntityMovementType.MOVE_NONE ? "Stationary" : "Flanking";
    }

    /**
     * Returns the abbreviation of the name of the type of movement used.
     *
     */
    @Override
    public String getMovementAbbr(EntityMovementType movementType) {
        return movementType == EntityMovementType.MOVE_NONE ? "N" : "F";
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
        return calculateBattleValue();
    }

    /**
     * The maximum elevation change the entity can cross
     */
    @Override
    public int getMaxElevationChange() {
        return getMovementMode() == EntityMovementMode.TRACKED ? 2 : 1;
    }

    public StructureEngine getPowerSystem() {
        return powerSystem;
    }

    public void setPowerSystem(StructureEngine power) {
        powerSystem = java.util.Objects.requireNonNull(power);
        setEngine(new Engine(0, power.getEngineType(), isClan() ? Engine.CLAN_ENGINE : 0));
    }

    public double getMaximumMP() {
        return maximumMPQuarters / 4.0;
    }

    public void setMaximumMP(double mp) {
        if (!Double.isFinite(mp) || mp < .25 || mp * 4 != Math.rint(mp * 4) || mp > 4) {
            throw new IllegalArgumentException("Mobile Structure MP must be 0.25 to 4 in quarter-point increments");
        }
        maximumMPQuarters = (int) (mp * 4);
        setOriginalWalkMP(maximumMPQuarters);
    }

    public int getMaximumMPQuarters() {
        return maximumMPQuarters;
    }

    public int motiveMaximumMP() {
        return switch (getMovementMode()) {
            case TRACKED -> 2;
            case NAVAL -> 3;
            case VTOL, SUBMARINE -> 4;
            default -> 0;
        };
    }

    /** The movement engine counts quarters for Mobile Structures, including terrain costs. */
    @Override
    public int getWalkMP(MPCalculationSetting setting) {
        return isImmobile() ? 0 : maximumMPQuarters;
    }

    @Override
    public int getRunMP(MPCalculationSetting setting) {
        return getWalkMP(setting);
    }

    @Override
    public int getSprintMP(MPCalculationSetting setting) {
        return getWalkMP(setting);
    }

    @Override
    public int getJumpMP(MPCalculationSetting setting) {
        return 0;
    }

    @Override
    public boolean isImmobile() {
        return mobileCrewHits >= 6 || isShutDown() || isPowerSwitchedOff();
    }

    public double getOperatingRange() {
        return operatingRange;
    }

    public void setOperatingRange(double kilometers) {
        if (!Double.isFinite(kilometers) || kilometers < 0) {
            throw new IllegalArgumentException("Operating range must be a non-negative number of kilometers");
        }
        operatingRange = kilometers;
    }

    public double getPowerSystemWeight() {
        return Math.ceil(getInternalBuilding().getOriginalCoordsList().size()
              * getInternalBuilding().getBuildingHeight() * getMaximumMP()
              * powerSystem.mobilePowerMultiplier(getMovementMode(), isClan()));
    }

    public double getMotiveSystemWeight() {
        double multiplier = switch (getMovementMode()) {
            case TRACKED -> isClan() ? 3.5 : 4;
            case VTOL -> isClan() ? 4 : 5;
            case NAVAL -> isClan() ? 1.8 : 2;
            case SUBMARINE -> 3.5;
            default -> 0;
        };
        double structureMultiplier = getBldgClass() == HANGAR ? .3 : getBldgClass() == STANDARD ? .5 : 1;
        return Math.ceil(getInternalBuilding().getOriginalCoordsList().size()
              * getInternalBuilding().getBuildingHeight() * multiplier * structureMultiplier);
    }

    public double getFuelWeight() {
        return Math.ceil(operatingRange / 100 * Math.max(0, powerSystem.getMobileFuelMultiplier()) * getPowerSystemWeight());
    }

    public double systemWeightInHex(CubeCoords hex) {
        int count = getInternalBuilding().getOriginalCoordsList().size();
        if (count == 0 || !getInternalBuilding().getOriginalCoordsList().contains(hex)) {
            return 0;
        }
        double sealing = getMovementMode() != EntityMovementMode.SUBMARINE && hasEnvironmentalSealing()
              ? Math.ceil(getOInternal(0) * getInternalBuilding().getBuildingHeight() / 10.0) : 0;
        return (getPowerSystemWeight() + getMotiveSystemWeight() + getFuelWeight()) / count + sealing;
    }

    @Override
    protected int calculateBaseCrew() {
        int crew = switch (getBldgClass()) {
            case HANGAR -> switch (getMovementMode()) {
                case VTOL, SUBMARINE -> 3;
                default -> 2;
            };
            case STANDARD -> switch (getMovementMode()) {
                case VTOL, SUBMARINE -> 4;
                default -> 3;
            };
            case FORTRESS -> switch (getMovementMode()) {
                case NAVAL -> 5;
                case SUBMARINE -> 6;
                case TRACKED -> 4;
                default -> 0;
            };
            default -> 0;
        };
        return crew * getInternalBuilding().getOriginalCoordsList().size();
    }

    @Override
    public boolean hasEnvironmentalSealing() {
        return getMovementMode() == EntityMovementMode.SUBMARINE || super.hasEnvironmentalSealing();
    }

    @Override
    public boolean hasFusionOrFissionPower() {
        return powerSystem == StructureEngine.FUSION || powerSystem == StructureEngine.FISSION;
    }

    public boolean hasPower() {
        return !isPowerSwitchedOff() && powerSystem.mobilePowerMultiplier(getMovementMode(), isClan()) > 0;
    }

    /** Relative elevation of the lowest usable floor; motive levels consume no equipment capacity. */
    public int getStructureBaseElevation() {
        return switch (getMovementMode()) {
            case TRACKED -> 2;
            case NAVAL, SUBMARINE -> -(getInternalBuilding().getBuildingHeight() + 1) / 2;
            default -> 0;
        };
    }

    @Override
    public int getWeaponFiringHeight(WeaponMounted weapon) {
        return getStructureBaseElevation() + super.getWeaponFiringHeight(weapon);
    }

    @Override
    public int height() {
        return Math.max(0, getInternalBuilding().getBuildingHeight() - 1 + Math.max(0, getStructureBaseElevation()));
    }

    public int getMobileCrewHits() {
        return mobileCrewHits;
    }

    /** TO:AUE p.40: six hits stop movement, but surviving individual gunners can still fire. */
    public void addMobileCrewHit() {
        mobileCrewHits = Math.min(6, mobileCrewHits + 1);
    }
}
