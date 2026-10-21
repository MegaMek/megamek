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

import megamek.common.MPCalculationSetting;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
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
    private MovementProgress movementProgress;
    private java.util.Map<CubeCoords, Double> fuelLocations = new java.util.LinkedHashMap<>();
    private int waterSpeedQuarters;
    private int speedDeclaredRound = Integer.MIN_VALUE;
    private int driftingTurns;
    private int depthChangesThisTurn;
    private int travelDirection;
    private boolean grounded;
    private MobileStructureNavalState navalState;
    private boolean airLandingGearDamaged;

    public boolean isAirLandingGearDamaged() { return airLandingGearDamaged; }

    public void setAirLandingGearDamaged(boolean damaged) { airLandingGearDamaged = damaged; }

    @Override
    public boolean isAirborneVTOLorWIGE() {
        return megamek.common.moves.MobileStructureAirMovement.isAirborne(this);
    }

    @Override
    public boolean canGoDown() {
        return canGoDown(getElevation(), getPosition(), getBoardId());
    }

    @Override
    public boolean canGoDown(int assumed, megamek.common.board.Coords coords, int boardId) {
        if (getGame() == null || getBoardId() != boardId || coords == null || isImmobile()
              || getMovementMode() != EntityMovementMode.SUBMARINE && getMovementMode() != EntityMovementMode.VTOL) {
            return false;
        }
        return megamek.common.moves.MobileStructureMovement.cost(getGame(), this, coords, getFacing(), assumed,
              coords, getFacing(), assumed - 1) != megamek.common.moves.MobileStructureMovement.PROHIBITED;
    }

    @Override
    public boolean canGoUp(int assumed, megamek.common.board.Coords coords, int boardId) {
        if (getGame() == null || getBoardId() != boardId || coords == null || isImmobile()
              || getMovementMode() != EntityMovementMode.SUBMARINE && getMovementMode() != EntityMovementMode.VTOL) {
            return false;
        }
        return megamek.common.moves.MobileStructureMovement.cost(getGame(), this, coords, getFacing(), assumed,
              coords, getFacing(), assumed + 1) != megamek.common.moves.MobileStructureMovement.PROHIBITED;
    }
    private java.util.List<megamek.common.moves.MobileStructureLinkage.Link> moduleLinks = new java.util.ArrayList<>();

    public java.util.List<megamek.common.moves.MobileStructureLinkage.Link> getModuleLinks() {
        return moduleLinks == null ? java.util.List.of() : java.util.List.copyOf(moduleLinks);
    }

    public void setModuleLinks(java.util.List<megamek.common.moves.MobileStructureLinkage.Link> links) {
        moduleLinks = new java.util.ArrayList<>(links);
    }

    @Override
    public void removeHex(megamek.common.board.Coords coords) {
        megamek.common.moves.MobileStructureLinkage.removeHex(this, boardToRelative(coords));
        super.removeHex(coords);
    }

    public MobileStructureNavalState getNavalState() {
        if (navalState == null) {
            navalState = new MobileStructureNavalState();
        }
        return navalState;
    }

    public record MovementProgress(megamek.common.board.Coords destination, int facing, int elevation, int quarters)
          implements java.io.Serializable { }

    public MovementProgress getMovementProgress() {
        return movementProgress;
    }

    public void cancelMovementProgress() {
        movementProgress = null;
    }

    /** Quarter points are committed to one move, never banked for a later unrelated maneuver. */
    public boolean advanceMovement(megamek.common.board.Coords destination, int facing, int elevation, int cost, int available) {
        int paid = movementProgress != null && movementProgress.destination().equals(destination)
              && movementProgress.facing() == facing && movementProgress.elevation() == elevation
              ? movementProgress.quarters() : 0;
        int expenditure = Math.min(available, Math.max(0, cost - paid));
        mpUsed += expenditure;
        paid += expenditure;
        movementProgress = paid >= cost ? null : new MovementProgress(destination, facing, elevation, paid);
        return paid >= cost;
    }

    public MobileStructure(BuildingType type, int bldgClass) {
        super(type, bldgClass);
        setMovementMode(EntityMovementMode.TRACKED);
        setPowerSystem(StructureEngine.FUSION);
        setMaximumMP(1);
    }

    /**
     * @see UnitType
     */
    @Override
    public int getUnitType() {
        return UnitType.MOBILE_STRUCTURE;
    }

    @Override
    public long getEntityType() {
        return ETYPE_BUILDING_ENTITY | ETYPE_MOBILE_STRUCTURE;
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
        if (grounded) {
            return 0;
        }
        if (isWaterStructure()) {
            return isImmobile() ? waterSpeedQuarters : Math.min(maximumMPQuarters, waterSpeedQuarters + 4);
        }
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
        return megamek.common.moves.MobileStructureLinkage.group(this).stream().anyMatch(MobileStructure::isModuleImmobile);
    }

    public boolean isModuleImmobile() {
        return getNavalState().isSinking() || grounded || mobileCrewHits >= 6 || isShutDown() || isPowerSwitchedOff();
    }

    public boolean isWaterStructure() {
        return getMovementMode().isNaval() || getMovementMode().isSubmarine();
    }

    public int getWaterSpeedQuarters() {
        return waterSpeedQuarters;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public void setGrounded(boolean value) {
        grounded = value;
        if (value) {
            waterSpeedQuarters = 0;
            cancelMovementProgress();
        }
    }

    /** Acceleration is declared once per turn. A disabled vessel coasts and loses one MP every other turn. */
    public int declareWaterSpeed(int requestedQuarters, int round) {
        if (speedDeclaredRound != round) {
            speedDeclaredRound = round;
            if (grounded) {
                waterSpeedQuarters = 0;
            } else if (isImmobile()) {
                if (++driftingTurns % 2 == 0) {
                    waterSpeedQuarters = Math.max(0, waterSpeedQuarters - 4);
                }
            } else {
                driftingTurns = 0;
                waterSpeedQuarters = Math.clamp(requestedQuarters, Math.max(0, waterSpeedQuarters - 4),
                      Math.min(maximumMPQuarters, waterSpeedQuarters + 4));
            }
        }
        return waterSpeedQuarters;
    }

    public int getDepthChangesThisTurn() {
        return depthChangesThisTurn;
    }

    public void recordDepthChange() {
        depthChangesThisTurn++;
    }

    public int getTravelDirection() {
        return travelDirection;
    }

    public void setTravelDirection(int direction) {
        travelDirection = Math.floorMod(direction, 6);
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);
        depthChangesThisTurn = 0;
    }

    @Override
    public boolean isEligibleForMovement() {
        if (getNavalState().isSinking()) {
            return false;
        }
        return (!megamek.common.moves.MobileStructureLinkage.partners(this).isEmpty() && !isDestroyed() && !isDoomed())
              || (isWaterStructure() && !grounded && waterSpeedQuarters > 0 && !isDestroyed() && !isDoomed())
              || super.isEligibleForMovement();
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
        return java.math.BigDecimal.valueOf(operatingRange)
              .multiply(java.math.BigDecimal.valueOf(Math.max(0, powerSystem.getMobileFuelMultiplier())))
              .multiply(java.math.BigDecimal.valueOf(getPowerSystemWeight()))
              .divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.CEILING).doubleValue();
    }

    /** An empty allocation means uniform fuel storage; explicit entries are tons in construction coordinates. */
    public java.util.Map<CubeCoords, Double> getFuelLocations() {
        if (fuelLocations == null) {
            fuelLocations = new java.util.LinkedHashMap<>();
        }
        return java.util.Collections.unmodifiableMap(fuelLocations);
    }

    public void setFuelLocations(java.util.Map<CubeCoords, Double> locations) {
        locations.forEach((hex, tons) -> {
            if (!getInternalBuilding().getOriginalCoordsList().contains(hex)
                  || !Double.isFinite(tons) || tons < 0) {
                throw new IllegalArgumentException("Fuel locations require an occupied hex and non-negative tons");
            }
        });
        fuelLocations = new java.util.LinkedHashMap<>(locations);
    }

    public double fuelWeightInHex(CubeCoords hex) {
        if (!getInternalBuilding().getOriginalCoordsList().contains(hex)) {
            return 0;
        }
        var fuel = getFuelLocations();
        return fuel.isEmpty() ? getFuelWeight() / getInternalBuilding().getOriginalCoordsList().size()
              : fuel.getOrDefault(hex, 0.0);
    }

    public double systemWeightInHex(CubeCoords hex) {
        int count = getInternalBuilding().getOriginalCoordsList().size();
        if (count == 0 || !getInternalBuilding().getOriginalCoordsList().contains(hex)) {
            return 0;
        }
        double sealing = getMovementMode() != EntityMovementMode.SUBMARINE && hasEnvironmentalSealing()
              ? Math.ceil(getOInternal(0) * getInternalBuilding().getBuildingHeight() / 10.0) : 0;
        // TO:AUE pp.79–80: each system is spread uniformly and rounded separately to the next half ton.
        return Math.ceil(getPowerSystemWeight() / count * 2) / 2
              + Math.ceil(getMotiveSystemWeight() / count * 2) / 2 + fuelWeightInHex(hex) + sealing;
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

    @Override
    public boolean requiresGunner(WeaponMounted weapon) {
        return super.requiresGunner(weapon) && weapon.getType().getLongRange() > 1;
    }

    /** Relative elevation of the lowest usable floor; motive levels consume no equipment capacity. */
    public int getStructureBaseElevation() {
        return switch (getMovementMode()) {
            case TRACKED -> 2;
            case NAVAL, SUBMARINE -> -(getInternalBuilding().getBuildingHeight() + 1) / 2;
            default -> 0;
        };
    }

    /** Ground motives follow the local bed; naval and air motives use their commanded surface-relative elevation. */
    public int getBaseElevation(megamek.common.board.Coords coords) {
        if (getMovementMode() == EntityMovementMode.VTOL) {
            return megamek.common.moves.MobileStructureAirMovement.translatedElevation(this, getPosition(), coords, getElevation());
        }
        if (getNavalState().isSinking()) {
            return getElevation() + getNavalState().getBaseOffsets()
                  .getOrDefault(boardToRelative(coords), getStructureBaseElevation());
        }
        if (getMovementMode() == EntityMovementMode.TRACKED && getGame() != null && coords != null) {
            Integer tunnelFloor = MobileStructurePortalRules.supportElevation(this, coords);
            if (tunnelFloor != null) {
                return tunnelFloor + getStructureBaseElevation();
            }
            var footprint = megamek.common.moves.MobileStructureLinkage.group(this).stream()
                  .flatMap(module -> module.getCoordsList().stream()).distinct().toList();
            int support = megamek.common.moves.MobileStructureSupport.level(getGame(), this, footprint, coords);
            var boardHex = getGame().getBoard(this).getHex(coords);
            int surface = boardHex == null
                  ? megamek.common.moves.MobileStructureMovement.terrain(getGame(), this, coords).getLevel()
                  : boardHex.getLevel();
            return getStructureBaseElevation() + support - surface;
        }
        return getElevation() + getStructureBaseElevation();
    }

    @Override
    public int height() {
        return Math.max(0, getInternalBuilding().getBuildingHeight() - 1 + getStructureBaseElevation());
    }

    public int getMobileCrewHits() {
        return mobileCrewHits;
    }

    /** Removing a complete interior row disconnects the surviving footprint (TO:AUE p.40). */
    public boolean isSplit() {
        var hexes = getInternalBuilding().getCoordsList().stream()
              .filter(hex -> getGame() == null || getPosition() == null
                    || getGame().getBoard(this).contains(relativeToBoard(hex))).toList();
        if (hexes.isEmpty()) {
            return true;
        }
        var visited = new java.util.HashSet<CubeCoords>();
        var pending = new java.util.ArrayDeque<CubeCoords>();
        pending.add(hexes.getFirst());
        while (!pending.isEmpty()) {
            var hex = pending.removeFirst();
            if (visited.add(hex)) {
                hex.neighbors().stream().filter(hexes::contains).filter(h -> !visited.contains(h)).forEach(pending::add);
            }
        }
        return visited.size() != hexes.size();
    }

    /** TO:AUE p.40: six hits stop movement, but surviving individual gunners can still fire. */
    public void addMobileCrewHit() {
        mobileCrewHits = Math.min(6, mobileCrewHits + 1);
    }
}
