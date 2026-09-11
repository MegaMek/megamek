/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2026 The MegaMek Team. All Rights Reserved.
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

import java.util.*;

import megamek.client.bot.princess.FireControl;
import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.IndustrialElevator;
import megamek.common.HitData;
import megamek.common.QuirkEntry;
import megamek.common.Report;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.actions.RepairWeaponMalfunctionAction;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.compute.InfantryActionStrengths;
import megamek.common.compute.InfantryCombatTables;
import megamek.common.cost.CostCalculator;
import megamek.common.cost.BuildingCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.PowerGeneratorType;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.BuildingEquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.exceptions.LocationFullException;
import megamek.common.equipment.enums.StructureEngine;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.rolls.PilotingRollData;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * AbstractBuildingEntity represents a non-terrain building (e.g., a moving fortress). This is the common implementation
 * of the Mobile Structure rules from TO:AUE and Advanced Building rules from TO:AUE.
 * <br>
 * It contains a {@link Building} (which stores data in relative coordinates) and handles translation between board
 * coordinates and the Building's relative coordinate space. Unlike BuildingTerrain, the translation is dynamic based on
 * Entity's current position/facing.
 */
public abstract class AbstractBuildingEntity extends Entity implements IBuilding {

    private static final MMLogger logger = MMLogger.create(AbstractBuildingEntity.class);

    private Building building;
    private final BuildingDesign design = new BuildingDesign();
    private WallSegmentState wallSegmentState;

    /** Independent combat state for authored hexsides; old saves initialize it from their undamaged design. */
    public WallSegmentState getWallSegmentState() {
        if (wallSegmentState == null) {
            wallSegmentState = new WallSegmentState();
        }
        return wallSegmentState;
    }

    public void copyWallSegmentState(AbstractBuildingEntity source) {
        wallSegmentState = new WallSegmentState(source.getWallSegmentState());
    }
    /**
     * Relative {@link CubeCoords} -> actual board {@link Coords}
     */
    private final Map<CubeCoords, Coords> relativeLayout = new HashMap<>();
    /**
     * Entity location -> relative {@link CubeCoords}
     */
    private final Map<Integer, CubeCoords> locationToRelativeCoordsMap = new HashMap<>();
    private static final int LOC_BASE = 0;

    public static final String[] HIT_LOCATION_NAMES = { "building" };

    private static final String LOCATION_ABBREVIATIONS_PREFIX = "LVL";
    private static final String LOCATION_NAMES_PREFIX = "Level";

    private static final int[] CRITICAL_SLOTS = new int[] { 100 };

    public AbstractBuildingEntity(BuildingType type, int bldgClass) {
        super();
        building = new Building(type, bldgClass, getId(), Terrains.BUILDING);

        initializeInternal(0, LOC_BASE);
    }

    @Override
    public CrewType defaultCrewType() {
        return CrewType.BUILDING;
    }

    // ========== IBuilding Coordinate Translation Overrides ==========

    @Override
    public Coords getBoardOrigin() {
        return getPosition();  // Entity's current position
    }

    @Override
    public int getBoardFacing() {
        return getFacing();  // Entity's current facing
    }

    @Override
    public Building getInternalBuilding() {
        return building;
    }

    /**
     * Changes an undamaged building's construction, retaining equipment at its hex and level. Equipment whose hex or
     * level is removed becomes unallocated. This is a construction operation, not a way to apply combat damage.
     */
    public void configureConstruction(BuildingType type, int buildingClass, int levels, int cf, int armor,
          List<CubeCoords> coordinates) {
        configureConstruction(type, buildingClass, levels, cf, armor, coordinates, java.util.function.UnaryOperator.identity(),
              facing -> facing);
    }

    /** Rebuild the construction and carry equipment and authored features through the same hex/facing transform. */
    public void configureConstruction(BuildingType type, int buildingClass, int levels, int cf, int armor,
          List<CubeCoords> coordinates, java.util.function.UnaryOperator<CubeCoords> hexTransform,
          java.util.function.IntUnaryOperator facingTransform) {
        if (levels < 1 || coordinates.isEmpty()
              || new HashSet<>(coordinates).size() != coordinates.size()
              || coordinates.stream().anyMatch(c -> c.q() != Math.rint(c.q()) || c.r() != Math.rint(c.r())
                    || c.s() != Math.rint(c.s()) || c.q() + c.r() + c.s() != 0)) {
            throw new IllegalArgumentException("A building needs whole cube coordinates and at least one hex and level.");
        }
        int oldHeight = Math.max(1, building.getBuildingHeight());
        wallSegmentState = null;
        Map<CubeCoords, Integer> oldHexHeights = new HashMap<>();
        Map<CubeCoords, Double> oldFuelLocations = this instanceof MobileStructure mobile
              ? new HashMap<>(mobile.getFuelLocations()) : Map.of();
        if (this instanceof MobileStructure) {
            building.getOriginalCoordsList().forEach(hex -> oldHexHeights.put(hexTransform.apply(hex), building.getHeight(hex)));
        }
        if (this instanceof BuildingEntity entity && BuildingConstruction.usesHexsides(entity)) {
            building.getOriginalCoordsList().forEach(hex -> entity.getDesign().getWallSides().putIfAbsent(hex, 1));
        }
        Map<Integer, CubeCoords> oldLocations = new HashMap<>(locationToRelativeCoordsMap);
        building = new Building(type, buildingClass, getId(), Terrains.BUILDING);
        building.setBuildingHeight(levels);
        CubeCoords origin = coordinates.contains(CubeCoords.ZERO) ? CubeCoords.ZERO : coordinates.getFirst();
        for (CubeCoords coords : coordinates) {
            CubeCoords relative = new CubeCoords((int) (coords.q() - origin.q()), (int) (coords.r() - origin.r()),
                  (int) (coords.s() - origin.s()));
            building.addHex(relative, cf, armor, BasementType.NONE, false);
            int previousHeight = oldHexHeights.getOrDefault(coords, oldHeight);
            building.setHeight(previousHeight == oldHeight ? levels : Math.min(previousHeight, levels), relative);
        }
        refreshLocations();
        refreshAdditionalLocations();
        for (int loc = 0; loc < locations(); loc++) {
            initializeInternal(cf, loc);
            initializeArmor(armor, loc);
        }
        for (Mounted<?> mount : getEquipment()) {
            int oldLocation = mount.getLocation();
            CubeCoords oldHex = oldLocations.get(oldLocation);
            int hexIndex = oldHex == null ? -1 : coordinates.indexOf(hexTransform.apply(oldHex));
            int level = oldLocation < 0 ? -1 : oldLocation % oldHeight;
            int location = hexIndex < 0 || level < 0 || level >= levels ? LOC_NONE : hexIndex * levels + level;
            mount.setLocation(location);
            if (mount.getFacing() >= 0) {
                mount.setFacing(facingTransform.applyAsInt(mount.getFacing()));
            }
            if (location != LOC_NONE) {
                addCritical(location, new CriticalSlot(mount));
            }
        }
        getDesign().remap(position -> {
                CubeCoords hex = hexTransform.apply(position.hex());
                int level = position.level() == oldHeight ? levels : position.level();
                return !coordinates.contains(hex) || (position.level() != oldHeight && level >= levels) ? null
                      : new BuildingDesign.Position(hex.subtract(origin), level);
        }, facingTransform);
        if (this instanceof MobileStructure mobile && !oldFuelLocations.isEmpty()) {
            Map<CubeCoords, Double> fuel = new HashMap<>();
            // Keep an explicit zero for surviving hexes so deleting the only fuel tank cannot silently redistribute it.
            building.getOriginalCoordsList().forEach(hex -> fuel.put(hex, 0.0));
            oldFuelLocations.forEach((hex, tons) -> {
                CubeCoords transformed = hexTransform.apply(hex);
                if (coordinates.contains(transformed)) {
                    fuel.put(transformed.subtract(origin), tons);
                }
            });
            mobile.setFuelLocations(fuel);
        }
        updateRelativeLayout();
        recalculateTechAdvancement();
    }

    @Override
    public @Nullable CubeCoords boardToRelative(@Nullable Coords boardCoords) {
        if (boardCoords == null) {
            return null;
        }
        // Find which relative CubeCoord maps to this board coordinate
        return relativeLayout.entrySet().stream()
              .filter(e -> e.getValue().equals(boardCoords))
              .map(Map.Entry::getKey)
              .findFirst()
              .orElse(boardCoords.toCube());
    }

    @Override
    public Coords relativeToBoard(CubeCoords relativeCoords) {
        // Return the board coordinate this relative CubeCoord maps to
        return relativeLayout.get(relativeCoords);
    }

    /**
     * Converts board coordinates to all entity location numbers at that hex. Returns all floors (locations) that exist
     * at the given coordinates.
     *
     * @param coords the board coordinates to convert
     *
     * @return list of all location numbers at these coords (one per floor)
     */
    private List<Integer> coordsToLocations(@Nullable Coords coords) {
        return locationsForRelativeCoords(boardToRelative(coords));
    }

    /**
     * Returns every entity location that sits at the given relative hex, one per floor.
     *
     * <p>This works in the building's own coordinate space rather than the board's, so it can be used before the
     * building is placed. In the lobby a building has no position, which leaves the board translation with nothing
     * to translate.</p>
     *
     * @param relativeCoords the hex in the building's relative coordinate space, or {@code null}
     *
     * @return the location numbers at that hex, or an empty list when the hex is not part of this building
     */
    private List<Integer> locationsForRelativeCoords(@Nullable CubeCoords relativeCoords) {
        if (relativeCoords == null) {
            return List.of();
        }

        List<Integer> locations = new ArrayList<>();
        for (Map.Entry<Integer, CubeCoords> entry : locationToRelativeCoordsMap.entrySet()) {
            if (entry.getValue().equals(relativeCoords)) {
                locations.add(entry.getKey());
            }
        }

        return locations;
    }

    @Override
    public Coords getWeaponFiringPosition(WeaponMounted weapon) {
        if (weapon == null) {
            return super.getWeaponFiringPosition(weapon);
        }
        int location = weapon.getLocation();
        Coords firingPos = relativeToBoard(locationToRelativeCoordsMap.get(location));
        if (firingPos == null) {
            return super.getWeaponFiringPosition(weapon);
        }
        return firingPos;
    }

    /**
     * What height is this weapon physically firing from?
     *
     * @param weapon {@link WeaponMounted}
     *
     * @return int
     */
    @Override
    public int getWeaponFiringHeight(WeaponMounted weapon) {
        if (weapon == null) {
            return super.getWeaponFiringHeight(weapon);
        }
        Coords coords = getLocationCoords(weapon.getLocation());
        int baseHeight = BuildingElevation.base(this, coords) - getElevation();
        int turretHeight = this instanceof MobileStructure ? building.getBuildingHeight() - 1 : getHeight(coords);
        return baseHeight + (weapon.isSponsonTurretMounted() ? turretHeight : getLocationLevel(weapon.getLocation()));
    }

    /**
     * Override setPosition to populate the relativeLayout map when the entity is placed. This establishes the mapping
     * between the building's internal relative coordinates and their actual board coordinates.
     */
    @Override
    public void setPosition(Coords position) {
        setPosition(position, true);
    }

    @Override
    public void setPosition(Coords position, boolean gameUpdate) {
        HashSet<Coords> oldPositions = getOccupiedCoords();
        super.setPosition(position, false);
        updateRelativeLayout();
        if (game != null && gameUpdate) {
            game.updateEntityPositionLookup(this, oldPositions);
        }
    }

    /**
     * Returns true when the given location cannot legally be entered or deployed into by this unit at the given
     * elevation or altitude. Also returns true when the location doesn't exist. Even when this method returns true, the
     * location need not be deadly to the unit.
     *
     * @param testPosition  The position to test
     * @param testBoardId   The board to test
     * @param testElevation The elevation or altitude to test
     *
     * @return True when the location is illegal to be in for this unit, regardless of elevation
     *
     * @see #isLocationDeadly(Coords)
     */
    @Override
    public boolean isLocationProhibited(Coords testPosition, int testBoardId, int testElevation) {
        return !isPositionAndFacingValid(testPosition, getFacing(), testElevation, testBoardId);
    }

    /**
     * Rotates a cube coordinate clockwise around the origin by the given facing. Facing 0 is UP (no rotation), and each
     * facing increment is 60° clockwise.
     *
     * @param coord  the CubeCoords to rotate
     * @param facing the facing direction (0-5), where 0 is UP and increments are 60° clockwise
     *
     * @return a new CubeCoords rotated by the given facing
     */
    private CubeCoords rotateCoordByFacing(CubeCoords coord, int facing) {
        // Normalize facing to 0-5 range
        int normalizedFacing = ((facing % 6) + 6) % 6;

        return switch (normalizedFacing) {
            case 0 -> coord; // No rotation
            case 1 -> new CubeCoords(-coord.r(), -coord.s(), -coord.q()); // 60° clockwise
            case 2 -> new CubeCoords(coord.s(), coord.q(), coord.r()); // 120° clockwise
            case 3 -> new CubeCoords(-coord.q(), -coord.r(), -coord.s()); // 180°
            case 4 -> new CubeCoords(coord.r(), coord.s(), coord.q()); // 240° clockwise
            case 5 -> new CubeCoords(-coord.s(), -coord.q(), -coord.r()); // 300° clockwise
            default -> coord; // Should never happen due to normalization
        };
    }

    /**
     * Computes what the relative layout would be for a hypothetical position and facing WITHOUT modifying the entity's
     * actual position or facing. This is a pure calculation method with no side effects.
     *
     * @param testPosition The position to test
     * @param testFacing   The facing to test (0-5)
     *
     * @return Map of relative CubeCoords to their board Coords at the given position/facing
     */
    public Map<CubeCoords, Coords> computeLayoutForPositionAndFacing(Coords testPosition, int testFacing) {
        Map<CubeCoords, Coords> hypotheticalLayout = new HashMap<>();

        if (testPosition == null || building == null) {
            return hypotheticalLayout;
        }

        // Map each relative CubeCoord to its hypothetical board coordinate
        for (CubeCoords relCoord : building.getCoordsList()) {
            CubeCoords rotatedRelCoord = rotateCoordByFacing(relCoord, testFacing);
            Coords boardCoord = testPosition.toCube().add(rotatedRelCoord).toOffset();
            hypotheticalLayout.put(relCoord, boardCoord);
        }

        return hypotheticalLayout;
    }

    /**
     * Checks if all hexes of this building would be valid at the given position and facing WITHOUT modifying the
     * entity's state. This is a pure calculation method.
     *
     * @param testPosition  The position to test
     * @param testFacing    The facing to test (0-5)
     * @param testElevation The elevation to test
     * @param testBoardId   The board ID to test
     *
     * @return true if all building hexes would be valid at this position/facing
     */
    public boolean isPositionAndFacingValid(Coords testPosition, int testFacing, int testElevation, int testBoardId) {
        if (game == null || !game.hasBoardLocation(testPosition, testBoardId)) {
            return false;
        }

        // Calculate where this building's hexes would be at testPosition with testFacing
        List<Coords> thisBuildingCoords = computeBuildingCoordsForPositionAndFacing(testPosition, testFacing);

        // Check that all hexes exist and are valid
        if (!areCoordsValid(thisBuildingCoords, testBoardId)) {
            return false;
        }

        if (BuildingConstruction.usesHexsides(this)
              && WallRules.hasOverlappingSegment(this, testPosition, testFacing, testBoardId)) {
            return false;
        }


        // Check that all hexes are at the same elevation
        if (!areAllCoordsAtSameElevation(thisBuildingCoords, testBoardId)) {
            return false;
        }

        // Check for overlapping buildings
        if (hasInvalidBuildingOverlap(thisBuildingCoords, testBoardId, testElevation)) {
            return false;
        }

        // Check for other entities at all positions
        if (hasEntityConflict(thisBuildingCoords, testBoardId, testElevation)) {
            return false;
        }

        // Check stacking violations at each position
        for (Coords coord : thisBuildingCoords) {
            if (Compute.stackingViolation(game, this, testElevation, coord,
                  testBoardId, null, climbMode(), true) != null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests a deployment without changing the building or the board. Every hex of the rotated footprint must fit
     * in the deployment zone, including hexes beyond the building's origin.
     */
    public boolean isDeploymentPositionAndFacingValid(Coords position, int facing, int elevation, int boardId) {
        return isPositionAndFacingValid(position, facing, elevation, boardId)
              && !isBoardProhibited(game.getBoard(boardId))
              && hasRequiredSurfacePart(position, facing, elevation, boardId)
              && (!(this instanceof MobileStructure mobile) || MobileStructurePortalRules.validDeployment(mobile, position, facing))
              && computeBuildingCoordsForPositionAndFacing(position, facing).stream()
                    .allMatch(coords -> game.getBoard(boardId).isLegalDeployment(coords, this));
    }

    /** A semi-subsurface design comprises co-located surface and subsurface structures (TO:AR p.139). */
    private boolean hasRequiredSurfacePart(Coords position, int facing, int elevation, int boardId) {
        int bottom = BuildingConstruction.baseLevel(this) + elevation;
        if (this instanceof MobileStructure || getDesign().getSite() == BuildingDesign.Site.SURFACE
              || bottom + getInternalBuilding().getBuildingHeight() != 0) {
            return true;
        }
        // A roof exactly at the surface is a connected basement/foundation rather than a freestanding
        // subsurface design. Deploy its surface portion first so its supporting limits can be verified.
        var footprint = computeBuildingCoordsForPositionAndFacing(position, facing);
        return footprint.stream().flatMap(coords -> game.getBoard(boardId).getBuildingsAt(coords).stream())
              .filter(AbstractBuildingEntity.class::isInstance).map(AbstractBuildingEntity.class::cast)
              .anyMatch(other -> other != this && other.getDesign().getSite() == BuildingDesign.Site.SURFACE
                    && BuildingElevation.base(other) == 0 && BuildingElevation.canCoexist(this, footprint, bottom, other));
    }

    /** Returns valid facings at a proposed position/elevation without changing this building's current pose. */
    public List<Integer> getValidFacingsAt(Coords testPosition, int testElevation, int testBoardId) {
        List<Integer> validFacings = new ArrayList<>();

        for (int facing = 0; facing < 6; facing++) {
            if (isPositionAndFacingValid(testPosition, facing, testElevation, testBoardId)) {
                validFacings.add(facing);
            }
        }

        return validFacings;
    }

    /**
     * Computes the board coordinates this building would occupy at the given position and facing.
     *
     * @param testPosition The position to test
     * @param testFacing   The facing to test
     *
     * @return List of all board coordinates the building would occupy
     */
    public List<Coords> computeBuildingCoordsForPositionAndFacing(Coords testPosition, int testFacing) {
        return getInternalBuilding().getCoordsList().stream()
              .map(cube -> testPosition.toCube().add(rotateCoordByFacing(cube, testFacing)).toOffset())
              .toList();
    }

    /**
     * Checks if all given coordinates are valid (exist on the board and have valid hexes).
     *
     * @param coords      The coordinates to check
     * @param testBoardId The board ID to check against
     *
     * @return true if all coordinates are valid
     */
    private boolean areCoordsValid(List<Coords> coords, int testBoardId) {
        Board board = game.getBoard(testBoardId);
        for (Coords coord : coords) {
            if (!game.hasBoardLocation(coord, testBoardId)) {
                return false;
            }
            if (board.getHex(coord) == null || board.getHex(coord).containsTerrain(Terrains.IMPASSABLE)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if all given coordinates are at the same elevation.
     *
     * @param coords      The coordinates to check
     * @param testBoardId The board ID to check against
     *
     * @return true if all coordinates are at the same elevation
     */
    private boolean areAllCoordsAtSameElevation(List<Coords> coords, int testBoardId) {
        if (coords.isEmpty()) {
            return true;
        }

        Board board = game.getBoard(testBoardId);
        Hex firstHex = board.getHex(coords.getFirst());
        if (firstHex == null) {
            return false;
        }
        int requiredElevation = firstHex.getLevel();

        for (Coords coord : coords) {
            Hex hex = board.getHex(coord);
            if (hex == null || hex.getLevel() != requiredElevation) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if this building would have an invalid overlap with other buildings. - Cannot overlap with another
     * AbstractBuildingEntity - Can only overlap with other IBuilding if we completely contain it
     *
     * @param thisBuildingCoords The coordinates this building would occupy
     * @param testBoardId        The board ID to check against
     *
     * @return true if there's an invalid overlap
     */
    private boolean hasInvalidBuildingOverlap(List<Coords> thisBuildingCoords, int testBoardId, int elevation) {
        // Collect all unique buildings we'd be overlapping with
        Set<IBuilding> overlappingBuildings = new HashSet<>();
        for (Coords coord : thisBuildingCoords) {
            game.getBoard(testBoardId).getBuildingsAt(coord).stream().filter(other -> !equals(other))
                  .forEach(overlappingBuildings::add);
        }

        // Check each overlapping building
        for (IBuilding otherBuilding : overlappingBuildings) {
            // Can't replace another AbstractBuildingEntity
            if (otherBuilding instanceof AbstractBuildingEntity) {
                if (!BuildingElevation.canCoexist(this, thisBuildingCoords,
                      BuildingConstruction.baseLevel(this) + elevation, otherBuilding)) {
                    return true;
                }
                continue;
            }

            // For any other IBuilding, we can only be placed if we contain ALL hexes of that building
            List<Coords> otherBuildingCoords = otherBuilding.getCoordsList();
            if (!thisBuildingCoords.containsAll(otherBuildingCoords)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if there are any entity conflicts at the given coordinates.
     *
     * @param coords      The coordinates to check
     * @param testBoardId The board to check
     *
     * @return true if there's an entity conflict (another entity at any of the coords)
     */
    private boolean hasEntityConflict(List<Coords> coords, int testBoardId, int elevation) {
        for (Coords coord : coords) {
            for (Entity otherEntity : game.getEntitiesVector(coord, testBoardId)) {
                if (!this.equals(otherEntity) && !(otherEntity instanceof IBuilding otherBuilding
                      && BuildingElevation.canCoexist(this, coords, BuildingConstruction.baseLevel(this) + elevation, otherBuilding))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Updates the relativeLayout map to reflect the current building configuration. Maps each relative CubeCoord in the
     * building to its actual board position.
     */
    private void updateRelativeLayout() {
        relativeLayout.clear();
        secondaryPositions.clear();

        if (getPosition() == null) {
            return;
        }
        int position = 0;

        if (getInternalBuilding() != null && getInternalBuilding().getHeight(CubeCoords.ZERO) > 0) {
            secondaryPositions.put(position++, getPosition());
        }
        relativeLayout.put(CubeCoords.ZERO, getPosition());

        // Map each relative CubeCoord to its actual board coordinate
        for (CubeCoords relCoord : building.getCoordsList()) {
            // We add the origin manually
            if (!relCoord.equals(CubeCoords.ZERO)) {
                // Rotate the relative coordinate by the entity's facing before adding to position
                CubeCoords rotatedRelCoord = rotateCoordByFacing(relCoord, getFacing());
                CubeCoords positionCubeCoords = getPosition().toCube();
                Coords boardCoord = positionCubeCoords.add(rotatedRelCoord).toOffset();

                relativeLayout.put(relCoord, boardCoord);
                if (getInternalBuilding() != null && getInternalBuilding().getHeight(relCoord) > 0) {
                    secondaryPositions.put(position++, boardCoord);
                }
            }
        }
    }

    /**
     * Returns the number of locations in the entity
     */
    @Override
    public int locations() {
        // Map can be null during construction
        if (locationToRelativeCoordsMap == null || locationToRelativeCoordsMap.isEmpty()) {
            return 1;
        }
        return locationToRelativeCoordsMap.size();
    }

    public void refreshAdditionalLocations() {
        armorType = new int[locations()];
        armorTechLevel = new int[locations()];
        hardenedArmorDamaged = new boolean[locations()];
        locationBlownOff = new boolean[locations()];
        locationBlownOffThisPhase = new boolean[locations()];
    }

    /**
     * Sets the primary facing.
     *
     */
    @Override
    public void setFacing(int facing) {
        HashSet<Coords> oldPositions = getOccupiedCoords();
        this.facing = FireControl.correctFacing(facing);
        updateRelativeLayout();
        if (game != null) {
            game.updateEntityPositionLookup(this, oldPositions);
            // Listeners must see the new footprint and position lookup when redrawing the building.
            game.processGameEvent(new GameEntityChangeEvent(this, this));
        }
    }

    /**
     * Can this entity change secondary facing at all?
     */
    @Override
    public boolean canChangeSecondaryFacing() {
        return false;
    }

    /**
     * Can this entity torso/turret twist the given direction?
     *
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        return false;
    }

    /**
     * Returns the closest valid secondary facing to the given direction.
     *
     * @return the closest valid secondary facing.
     */
    @Override
    public int clipSecondaryFacing(int dir) {
        return 0;
    }

    @Override
    public String[] getLocationNames() {
        return getLocationStrings(LOCATION_NAMES_PREFIX, true);
    }

    @Override
    public String[] getLocationAbbreviations() {
        return getLocationStrings(LOCATION_ABBREVIATIONS_PREFIX, true);
    }

    public String getLevelLabel(int level) {
        return getLevelLabel(level, false);
    }

    /** Display numbering only; construction locations retain their native floor indices. */
    public String getLevelLabel(int level, boolean compact) {
        long displayed = (long) BuildingConstruction.baseLevel(this) + level;
        return displayed == 0 ? (compact ? "G" : "Ground") : Long.toString(displayed);
    }

    /** Stable equipment block name in a building design, independent of placement and facing. */
    public String getConstructionLocationName(int location) {
        return getLocationStrings(LOCATION_NAMES_PREFIX, false)[location];
    }

    /** Stable location abbreviation for design weapon quirks. */
    public String getConstructionLocationAbbr(int location) {
        return location < 0 ? getLocationAbbr(location) : getLocationStrings(LOCATION_ABBREVIATIONS_PREFIX, false)[location];
    }

    @Override
    protected Mounted<?> getEquipmentForWeaponQuirk(QuirkEntry quirkEntry) {
        // Weapon quirks address the serialized construction location, not its ground-relative display label.
        for (int location = 0; location < locations(); location++) {
            if (getConstructionLocationAbbr(location).equalsIgnoreCase(quirkEntry.location())) {
                var critical = getCritical(location, quirkEntry.slot());
                return critical == null ? null : critical.getMount();
            }
        }
        return null;
    }

    private String[] getLocationStrings(String locationPrefix, boolean boardCoordinates) {
        ArrayList<String> locationAbbrvNames = new ArrayList<>();
        if (getInternalBuilding() == null || getInternalBuilding().getOriginalCoordsList() == null) {
            return new String[] { locationPrefix + ' ' + LOC_BASE };
        }
        for (int location = 0; location < locationToRelativeCoordsMap.size(); location++) {
            CubeCoords cubeCoords = locationToRelativeCoordsMap.get(location);
            String coordString;
            if (!boardCoordinates || getPosition() == null) {
                coordString = cubeCoords.q() + "," + cubeCoords.r() + "," + cubeCoords.s();
            } else {
                coordString = getPosition().toCube().add(rotateCoordByFacing(cubeCoords, getFacing()))
                      .toOffset().getBoardNum();
            }

            // Result is 0 indexed
            int level = (location % getInternalBuilding().getBuildingHeight());
            String label = boardCoordinates
                  ? getLevelLabel(getBldgClass() == IBuilding.BRIDGE ? getDesign().bridgeDeck(cubeCoords) : level)
                  : Integer.toString(level);
            locationAbbrvNames.add(locationPrefix + ' ' + label + ' ' + coordString);
        }
        return locationAbbrvNames.toArray(new String[0]);
    }

    @Override
    public int sideTable(Coords src) {
        return ToHitData.SIDE_FRONT;
    }

    /**
     * Rolls the to-hit number
     *
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        if (aimedLocation >= 0 && aimedLocation < locations() && !aimingMode.isNone()) {
            int roll = Compute.d6(2);
            HitData hit = new HitData(aimedLocation, false, roll >= 6 && roll <= 8);
            hit.setAimedShotAttempt(true);
            if (!hit.hitAimedLocation() && usesExpandedCF()) {
                List<Integer> candidates = getLocationsAt(getLocationCoords(aimedLocation)).stream()
                      .filter(location -> getInternal(location) > 0).toList();
                if (!candidates.isEmpty()) {
                    hit.setLocation(candidates.get(Compute.randomInt(candidates.size())));
                }
            }
            return hit;
        }
        return rollHitLocation(table, side);
    }

    /**
     * Rolls up a hit location
     *
     */
    @Override
    public HitData rollHitLocation(int table, int side) {
        return new HitData(LOC_BASE, false, HitData.EFFECT_NONE);
    }

    /**
     * Gets the location that excess damage transfers to. That is, one location inwards.
     *
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        return hit;
    }

    /**
     * Sets the internal structure for every location to appropriate undamaged values for the unit and location.
     */
    @Override
    public void autoSetInternal() {
        initializeInternal(0, LOC_BASE);
    }

    /**
     * Returns the Rules.ARC that the weapon, specified by number, fires into.
     *
     * @param weaponNumber integer equipment number, index from equipment list
     *
     * @return arc the specified weapon is in
     */
    @Override
    public int getWeaponArc(int weaponNumber) {
        WeaponMounted weapon = getWeapon(weaponNumber);
        if (isTurretMounted(weapon) && !isTurretLocked(weapon) && !isTurretJammed(weapon)) {
            return 0;
        }
        // A locked turret retains the weapon's facing, just like a fixed weapon in that direction.
        return switch (weapon.getFacing()) {
            case 0 -> 1;
            case 1 -> 50;
            case 2 -> 51;
            case 3 -> 52;
            case 4 -> 53;
            case 5 -> 54;
            default -> 0;
        };
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc. If false, assume it fires into the primary.
     *
     */
    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        return false;
    }

    @Override
    public int[] getNoOfSlots() {
        int[] slots = new int[locations()];
        for (int location = 0; location < slots.length; location++) {
            slots[location] = getNumberOfCriticalSlots(location);
        }
        return slots;
    }

    @Override
    public int getNumberOfCriticalSlots(int location) {
        if (location < 0 || location >= locations()) {
            return 0;
        }
        return crits != null && location < crits.length && crits[location] != null
              ? crits[location].length : CRITICAL_SLOTS[0];
    }

    /** Structures have a mass limit, not a critical-slot limit. Grow the backing hit-location list as required. */
    @Override
    public boolean addCritical(int location, CriticalSlot slot) {
        if (super.addCritical(location, slot)) {
            return true;
        }
        if (location < 0 || location >= locations()) {
            return false;
        }
        int index = crits[location].length;
        crits[location] = Arrays.copyOf(crits[location], index + Math.max(16, index / 2));
        setCritical(location, index, slot);
        return true;
    }

    @Override
    public void addEquipment(Mounted<?> mounted, int loc, boolean rearMounted)
          throws LocationFullException {
        super.addEquipment(mounted, loc, rearMounted);
        // Add the piece equipment to our slots.
        if (loc != LOC_NONE) {
            addCritical(loc, new CriticalSlot(mounted));
        }
    }

    /**
     * Calculates a "generic" Battle Value that is based on the average of all units of this type and tonnage. The
     * purpose of this generic Battle Value is to allow a comparison of this unit's actual BV to that for units of its
     * class. This can be used to balance forces without respect to unit or pilot quality.
     * <p>
     * <p>
     * /** Generates a vector containing reports on all useful information about this entity.
     */
    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<>();

        Report report = new Report(7025);
        report.type = Report.PUBLIC;
        report.addDesc(this);
        vDesc.addElement(report);

        report = new Report(7036);
        report.type = Report.PUBLIC;
        report.newlines = 0;
        vDesc.addElement(report);
        vDesc.addAll(getCrew().getDescVector(false));
        report = new Report(7070, Report.PUBLIC);
        report.add(getKillNumber());
        vDesc.addElement(report);

        if (isDestroyed()) {
            Entity killer = game.getEntity(killerId);
            if (killer == null) {
                killer = game.getOutOfGameEntity(killerId);
            }
            if (killer != null) {
                report = new Report(7072, Report.PUBLIC);
                report.addDesc(killer);
            } else {
                report = new Report(7073, Report.PUBLIC);
            }
            vDesc.addElement(report);
            report.newlines = 2;
        } else if (getCrew().isEjected()) {
            report = new Report(7071, Report.PUBLIC);
            vDesc.addElement(report);
            report.newlines = 2;
        }


        return vDesc;
    }

    /**
     * Add in any piloting skill mods
     *
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        return roll;
    }

    @Override
    public boolean isRepairable() {
        return isSalvage();
    }

    @Override
    public boolean isTargetable() {
        return false;
    }

    @Override
    public boolean canCharge() {
        return false;
    }

    @Override
    public boolean canFlee(Coords position) {
        return false;
    }

    @Override
    public boolean canGoDown() {
        return false;
    }

    @Override
    public boolean canGoDown(int assumed, Coords coords, int boardId) {
        return false;
    }

    @Override
    public double getCost(CalculationReport report, boolean ignoreAmmo) {
        return BuildingCostCalculator.calculateCost(this, report, ignoreAmmo);
    }

    @Override
    public boolean isNuclearHardened() {
        return false;
    }

    @Override
    public int getTotalCommGearTons() {
        return getExtraCommGearTons();
    }

    @Override
    public int getEngineHits() {
        return 0;
    }

    /**
     * Buildings are salvageable unless they have completely collapsed. A building has completely collapsed when all
     * hexes have 0 CF.
     */
    @Override
    public boolean isSalvage() {
        // Building is salvageable if it has any remaining structure
        return calculateTotalCurrentCF() > 0;
    }

    @Override
    public boolean isCrippled() {
        return isCrippled(true);
    }

    /**
     * Returns TRUE if the entity meets the requirements for crippling damage as detailed in TW pg 258. Excepting dead
     * or non-existing crew issues
     *
     * @return boolean
     */
    @Override
    public boolean isCrippled(boolean checkCrew) {
        // Building is crippled if it's military and all weapons are disabled
        return isMilitary() && !hasViableWeapons();
    }

    /**
     * Returns TRUE if the entity has been heavily damaged.
     *
     * @return boolean
     */
    @Override
    public boolean isDmgHeavy() {
        // Heavy damage: 50% or less of original structure
        return getStructurePercentage() <= 0.5;
    }

    /**
     * Returns TRUE if the entity has been moderately damaged.
     *
     * @return boolean
     */
    @Override
    public boolean isDmgModerate() {
        // Moderate damage: 75% or less (but more than 50%)
        double pct = getStructurePercentage();
        return pct <= 0.75 && pct > 0.5;
    }

    /**
     * Returns TRUE if the entity has been lightly damaged.
     *
     * @return boolean
     */
    @Override
    public boolean isDmgLight() {
        // Light damage: less than 100% (but more than 75%)
        double pct = getStructurePercentage();
        return pct < 1.0 && pct > 0.75;
    }

    /**
     * Calculate the percentage of remaining structure compared to original.
     *
     * @return percentage from 0.0 to 1.0
     */
    private double getStructurePercentage() {
        int currentCF = calculateTotalCurrentCF();
        int originalCF = calculateTotalOriginalCF();

        if (originalCF == 0) {
            return 1.0;
        }

        return (double) currentCF / (double) originalCF;
    }

    /**
     * Calculate total current CF across all hexes and levels.
     *
     * @return total current CF
     */
    private int calculateTotalCurrentCF() {
        if (building == null || building.getCoordsList() == null) {
            return 0;
        }

        int total = 0;
        for (CubeCoords coords : building.getCoordsList()) {
            total += building.getCurrentCF(coords);
        }
        return total;
    }

    /**
     * Calculate total original CF across all hexes and levels. Uses the original coords list to determine original
     * structure.
     *
     * @return total original CF
     */
    private int calculateTotalOriginalCF() {
        if (building == null || building.getOriginalCoordsList() == null) {
            return 0;
        }

        // Sum CF from all original coordinates
        int total = 0;
        for (CubeCoords coords : building.getOriginalCoordsList()) {
            // Get CF for this coordinate (using phase CF as reference for original)
            total += building.getPhaseCF(coords);
        }
        return total;
    }

    @Override
    public int getArmorType(int loc) {
        return 0;
    }

    @Override
    public int getArmorTechLevel(int loc) {
        return isClan() ? TechConstants.T_CLAN_TW : TechConstants.T_INTRO_BOX_SET;
    }

    @Override
    public boolean hasStealth() {
        return false;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_BUILDING_ENTITY;
    }

    @Override
    public int getInternalForReal(int loc) {
        if (locationToRelativeCoordsMap.containsKey(loc)) {
            CubeCoords relativeCoords = locationToRelativeCoordsMap.get(loc);
            BuildingFloorState floors = building.getFloorState(relativeCoords);
            if (floors != null) {
                return floors.getPhaseCF(loc % building.getBuildingHeight());
            }
            return getInternalBuilding().getPhaseCF(relativeCoords);
        }
        return 0;
    }

    /**
     * Returns the amount of armor in the location specified, or IArmorState.ARMOR_NA, or IArmorState.ARMOR_DESTROYED.
     *
     */
    @Override
    public int getArmor(int loc, boolean rear) {
        if (locationToRelativeCoordsMap.containsKey(loc)) {
            CubeCoords relativeCoords = locationToRelativeCoordsMap.get(loc);
            BuildingFloorState floors = building.getFloorState(relativeCoords);
            if (floors != null) {
                return floors.getArmor(loc % building.getBuildingHeight());
            }
            return getInternalBuilding().getArmor(relativeCoords);
        }
        return IArmorState.ARMOR_NA;
    }

    /**
     * Sets armor on both the entity and the wrapped {@link Building}, keeping the two in step.
     *
     * @param armor  the armor value to set
     * @param coords board coordinates of the hex whose armor is being set
     */
    private void setArmorInternal(int armor, Coords coords) {
        setArmorForRelativeCoords(armor, boardToRelative(coords));
    }

    /**
     * Sets armor for one hex of this building, on both the entity and the wrapped {@link Building}.
     *
     * <p>A building holds one armor value per hex rather than one per floor, so this writes the entity's armor for
     * every floor at that hex as well as the building's own value. Reads come back through
     * {@link #getArmor(int, boolean)}, which answers from the building, so a write that skipped the building would
     * simply be lost.</p>
     *
     * @param armor          the armor value to set; an {@link IArmorState} sentinel is translated first
     * @param relativeCoords the hex in the building's relative coordinate space, or {@code null} to do nothing
     */
    private void setArmorForRelativeCoords(int armor, @Nullable CubeCoords relativeCoords) {
        if (relativeCoords == null) {
            logger.debug("[BuildingDamage] {}: armor set to {} ignored, that location is not part of a hex yet",
                  getShortName(), armor);
            return;
        }

        int buildingArmor = withoutArmorStateSentinel(armor);
        for (int location : locationsForRelativeCoords(relativeCoords)) {
            super.setArmor(buildingArmor, location, false);
        }
        building.setArmor(buildingArmor, relativeCoords);
    }

    /**
     * Sets internal structure - a building hex's Construction Factor - on both the entity and the wrapped
     * {@link Building}, keeping the two in step.
     *
     * @param internal the Construction Factor to set
     * @param coords   board coordinates of the hex being set
     */
    private void setInternalInternal(int internal, Coords coords) {
        setConstructionFactorForRelativeCoords(internal, boardToRelative(coords));
    }

    /**
     * Sets the Construction Factor for one hex of this building, on both the entity and the wrapped
     * {@link Building}.
     *
     * @param constructionFactor the Construction Factor to set; an {@link IArmorState} sentinel is translated first
     * @param relativeCoords     the hex in the building's relative coordinate space, or {@code null} to do nothing
     */
    private void setConstructionFactorForRelativeCoords(int constructionFactor,
          @Nullable CubeCoords relativeCoords) {
        if (relativeCoords == null) {
            logger.debug("[BuildingDamage] {}: Construction Factor set to {} ignored, that location is not part of"
                  + " a hex yet", getShortName(), constructionFactor);
            return;
        }

        int buildingConstructionFactor = withoutArmorStateSentinel(constructionFactor);
        for (int location : locationsForRelativeCoords(relativeCoords)) {
            super.setInternal(buildingConstructionFactor, location);
        }
        building.setPhaseCF(buildingConstructionFactor, relativeCoords);
    }

    /**
     * Translates the {@link IArmorState} sentinels the damage code uses into a value a building hex can hold.
     *
     * <p>A Mek location can be blown clean off, which {@link IArmorState} records as a negative number. A building
     * hex has no such state: once its armor or Construction Factor is gone it stands at zero, and {@link Building}
     * rejects a negative value outright. Anything negative therefore becomes {@code 0} here.</p>
     *
     * @param value the value about to be stored, which may be an {@link IArmorState} sentinel
     *
     * @return {@code value} when it is zero or greater, otherwise {@code 0}
     */
    private static int withoutArmorStateSentinel(int value) {
        return Math.max(value, 0);
    }

    /**
     * Override to keep entity armor and building armor synchronized.
     *
     * <p>Without this, a write would land only in the entity's armor array while {@link #getArmor(int, boolean)}
     * kept answering from the building, so the new value would never be seen - which is what made damage edits
     * from the unit editor appear to do nothing.</p>
     */
    @Override
    public void setArmor(int value, int location, boolean rear) {
        if (rear) {
            // A building hex holds a single armor value and has no rear facing, so a rear write must not stand in
            // for the hex's real armor
            super.setArmor(value, location, true);
            return;
        }
        BuildingFloorState floors = building == null ? null
              : building.getFloorState(locationToRelativeCoordsMap.get(location));
        if (floors != null) {
            floors.setArmor(location % building.getBuildingHeight(), value);
            return;
        }
        setArmorForRelativeCoords(value, locationToRelativeCoordsMap.get(location));
    }

    /**
     * Override to keep entity internal and building CF synchronized.
     */
    @Override
    public void setInternal(int value, int location) {
        BuildingFloorState floors = building == null ? null
              : building.getFloorState(locationToRelativeCoordsMap.get(location));
        if (floors != null) {
            int floor = location % building.getBuildingHeight();
            floors.setCF(floor, value);
            floors.setPhaseCF(floor, value);
            return;
        }
        setConstructionFactorForRelativeCoords(value, locationToRelativeCoordsMap.get(location));
    }

    @Override
    public boolean hasCFIn(Coords coords) {
        CubeCoords relative = boardToRelative(coords);
        return relative != null && building.hasCFIn(relative);
    }

    @Override
    public Enumeration<Coords> getCoords() {
        // Return board coords by translating all relative coords
        Vector<Coords> boardCoords = new Vector<>();
        for (CubeCoords relCoord : building.getCoordsList()) {
            if (!building.hasCFIn(relCoord)) {
                continue;
            }
            Coords boardCoord = relativeToBoard(relCoord);
            if (boardCoord != null) {
                boardCoords.add(boardCoord);
            }
        }
        return boardCoords.elements();
    }

    @Override
    public List<Coords> getCoordsList() {
        // Return board coords by translating all relative coords
        return building.getCoordsList().stream()
              .filter(building::hasCFIn)
              .map(this::relativeToBoard)
              .filter(Objects::nonNull)
              .toList();
    }

    @Override
    public BuildingType getBuildingType() {
        return building.getBuildingType();
    }

    @Override
    public int getBldgClass() {
        return building.getBldgClass();
    }

    @Override
    public boolean getBasementCollapsed(Coords coords) {
        return building.getBasementCollapsed(boardToRelative(coords));
    }

    @Override
    public void collapseBasement(Coords coords, Board board, Vector<Report> vPhaseReport) {
        CubeCoords relative = boardToRelative(coords);
        building.collapseBasement(relative, board, vPhaseReport);
        // Update the board hex
        board.getHex(coords).addTerrain(new Terrain(Terrains.BLDG_BASE_COLLAPSED, 1));
    }

    @Override
    public boolean rollBasement(Coords coords, Board board, Vector<Report> vPhaseReport) {
        CubeCoords relative = boardToRelative(coords);
        boolean changed = building.rollBasement(relative, board, vPhaseReport);
        if (changed) {
            // Update the board hex with the rolled basement type
            BasementType rolledType = building.getBasement(relative);
            board.getHex(coords).addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE, rolledType.ordinal()));
        }
        return changed;
    }

    @Override
    public int getCurrentCF(Coords coords) {
        return building.getCurrentCF(boardToRelative(coords));
    }

    @Override
    public int getPhaseCF(Coords coords) {
        return building.getPhaseCF(boardToRelative(coords));
    }

    @Override
    public int getArmor(Coords coords) {
        return building.getArmor(boardToRelative(coords));
    }

    @Override
    public void setCurrentCF(int cf, Coords coords) {
        building.setCurrentCF(cf, boardToRelative(coords));
    }

    @Override
    public void setPhaseCF(int cf, Coords coords) {
        // Set internal structure for all locations (floors) at this hex
        // The private method will handle setting both building CF and entity internal
        setInternalInternal(cf, coords);
    }

    @Override
    public void setArmor(int a, Coords coords) {
        // Set armor for all locations (floors) at this hex
        // The private method will handle setting both building armor and entity armor
        setArmorInternal(a, coords);
    }

    @Override
    public int getHeight(Coords coords) {
        return building.getHeight(boardToRelative(coords));
    }

    @Override
    public void setHeight(int h, Coords coords) {
        building.setHeight(h, boardToRelative(coords));
    }

    @Override
    public String getName() {
        return building.getName();
    }

    @Override
    public boolean isBurning(Coords coords) {
        return building.isBurning(boardToRelative(coords));
    }

    @Override
    public void setBurning(boolean onFire, Coords coords) {
        building.setBurning(onFire, boardToRelative(coords));
    }

    /**
     * Demolition charges anchor to an absolute board hex, which only works for structures that cannot move.
     * {@link BuildingEntity} overrides this with a real implementation; {@link MobileStructure} must provide its own
     * (or remain unsupported) if mobile structures are ever ruled to be valid demolition targets.
     *
     * @throws UnsupportedOperationException always; subclasses that support demolition charges must override
     */
    @Override
    public void addDemolitionCharge(int playerId, int damage, Coords pos) {
        throw new UnsupportedOperationException(
              "Demolition charges are not supported on " + getClass().getSimpleName()
                    + "; only immobile structures can have charges placed on them");
    }

    @Override
    public void removeDemolitionCharge(DemolitionCharge charge) {
        building.removeDemolitionCharge(charge);
    }

    @Override
    public List<DemolitionCharge> getDemolitionCharges() {
        return building.getDemolitionCharges();
    }

    @Override
    public void setDemolitionCharges(List<DemolitionCharge> charges) {
        building.setDemolitionCharges(charges);
    }

    @Override
    public void removeHex(Coords coords) {
        CubeCoords relative = boardToRelative(coords);
        building.removeHex(relative);
        // Remove from layout
        //relativeLayout.remove(relative);
    }

    @Override
    public int getOriginalHexCount() {
        return building.getOriginalHexCount();
    }

    @Override
    public int getCollapsedHexCount() {
        return building.getCollapsedHexCount();
    }

    @Override
    public BasementType getBasement(Coords coords) {
        return building.getBasement(boardToRelative(coords));
    }

    @Override
    public void setBasement(Coords coords, BasementType basement) {
        building.setBasement(boardToRelative(coords), basement);
    }

    @Override
    public void setBasementCollapsed(Coords coords, boolean collapsed) {
        building.setBasementCollapsed(boardToRelative(coords), collapsed);
    }

    /**
     * Once a building entity has set its position, we need to update the board itself and share that with the clients
     *
     */
    public void updateBuildingEntityHexes(int boardId, TWGameManager gameManager) {
        Board board = getGame().getBoard(boardId);
        if (BuildingConstruction.usesHexsides(this)) {
            // Hexsides have no occupied volume. In particular, retain buildings and roads beside the wall.
            getWallSegmentState().initialize(this);
            board.addBuildingToBoard(this);
        for (MobileStructure portal : MobileStructurePortalRules.published(getGame())) {
            if (portal != this) {
                gameManager.entityUpdate(portal.getId());
            }
        }
            gameManager.entityUpdate(getId());
            return;
        }
        Vector<IBuilding> removedBuildings = new Vector<>();

        for (Coords buildingCoords : getCoordsList()) {
            Hex targetHex = board.getHex(buildingCoords);
            if (targetHex != null) {
                // Remove any existing building at this hex
                Optional<IBuilding> existingBuilding = getGame().getBuildingAt(buildingCoords, boardId);
                if (existingBuilding.isPresent() && !existingBuilding.get().equals(this)
                      && !BuildingElevation.canCoexist(this, existingBuilding.get())) {
                    removedBuildings.add(existingBuilding.get());
                    targetHex.removeTerrain(Terrains.BUILDING);
                    targetHex.removeTerrain(Terrains.BLDG_CF);
                    targetHex.removeTerrain(Terrains.BLDG_ELEV);
                    targetHex.removeTerrain(Terrains.BLDG_CLASS);
                    targetHex.removeTerrain(Terrains.BLDG_ARMOR);
                    targetHex.removeTerrain(Terrains.BLDG_BASEMENT_TYPE);
                }

                // Add building terrain with the building type
                if (getBldgClass() == BRIDGE) {
                    boolean sharedVolume = board.getBuildingsAt(buildingCoords).stream()
                          .anyMatch(other -> other != this && other.getBldgClass() != BRIDGE
                                && other.getBldgClass() != WALL && other.getBldgClass() != FENCE
                                && BuildingElevation.canCoexist(this, other));
                    if (!sharedVolume) {
                        targetHex.removeTerrain(Terrains.BUILDING);
                        targetHex.removeTerrain(Terrains.BLDG_ELEV);
                        targetHex.removeTerrain(Terrains.BLDG_CF);
                    }
                    int exits = 0;
                    for (int side = 0; side < 6; side++) {
                        if (isIn(buildingCoords.translated(side))) {
                            exits |= 1 << side;
                        }
                    }
                    targetHex.addTerrain(new Terrain(Terrains.BRIDGE, getBuildingType().getTypeValue(), true, exits));
                    targetHex.addTerrain(new Terrain(Terrains.BRIDGE_CF, getCurrentCF(buildingCoords)));
                    targetHex.addTerrain(new Terrain(Terrains.BRIDGE_ELEV, BuildingElevation.base(this, buildingCoords)));
                    continue;
                }
                // Entirely buried buildings are gameplay volumes, not a solid building placed on the ground above.
                if (BuildingElevation.roof(this, buildingCoords) <= 0) {
                    continue;
                }
                // The terrain envelope must retain an enclosing dome's roof when an interior structure is added.
                if (existingBuilding.isPresent() && existingBuilding.get() != this
                      && existingBuilding.get().getBldgClass() != BRIDGE
                      && BuildingElevation.canCoexist(this, existingBuilding.get())
                      && BuildingElevation.roof(existingBuilding.get(), buildingCoords)
                            > BuildingElevation.roof(this, buildingCoords)) {
                    continue;
                }
                targetHex.addTerrain(new Terrain(Terrains.BUILDING,
                      getBuildingType().getTypeValue()));

                // Add building class
                targetHex.addTerrain(new Terrain(Terrains.BLDG_CLASS, getBldgClass()));

                // Add CF value
                int cf = getCurrentCF(buildingCoords);
                targetHex.addTerrain(new Terrain(Terrains.BLDG_CF, cf));

                // Add armor if present
                int armor = getArmor(buildingCoords);
                if (armor > 0) {
                    targetHex.addTerrain(new Terrain(Terrains.BLDG_ARMOR, armor));
                }

                // Add height (BLDG_ELEV)
                int height = BuildingElevation.roof(this, buildingCoords);
                targetHex.addTerrain(new Terrain(Terrains.BLDG_ELEV, height));

                // Add basement type if present
                if (getBasement(buildingCoords) != null) {
                    targetHex.addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE,
                          getBasement(buildingCoords).ordinal()));
                }
            }
        }

        board.addBuildingToBoard(this);
        for (MobileStructure portal : MobileStructurePortalRules.published(getGame())) {
            if (portal != this) {
                gameManager.entityUpdate(portal.getId());
            }
        }

        for (var shaft : getDesign().getElevators()) {
            Coords coords = relativeToBoard(shaft.hex());
            if (!isIn(coords) || board.getHex(coords) == null) {
                continue;
            }
            int base = BuildingElevation.base(this, coords);
            BoardLocation location = BoardLocation.of(coords, boardId);
            var elevator = getGame().getIndustrialElevators().stream()
                  .filter(lift -> lift.getLocation().equals(location) && lift.getBuildingId() == getId()
                        && lift.getShaftBottom() == shaft.lowerLevel() + base).findFirst().orElse(null);
            if (elevator == null) {
                elevator = getGame().getIndustrialElevators().stream()
                      .filter(lift -> lift.getLocation().equals(location) && lift.getBuildingId() == Entity.NONE
                            && lift.getShaftBottom() == shaft.lowerLevel() + base && lift.getShaftTop() == shaft.upperLevel() + base
                            && lift.getCapacityTons() == shaft.capacity()).findFirst().orElse(null);
                if (elevator != null) {
                    elevator.setBuildingId(getId());
                }
            }
            if (elevator == null) {
                elevator = new IndustrialElevator(location, shaft.lowerLevel() + base, shaft.upperLevel() + base,
                      shaft.capacity());
                Map<Integer, Integer> sides = new HashMap<>();
                shaft.exits().forEach((level, mask) -> {
                    int rotated = 0;
                    for (int side = 0; side < 6; side++) {
                        if ((mask & (1 << side)) != 0) {
                            rotated |= 1 << ((side + getFacing()) % 6);
                        }
                    }
                    sides.put(level + base, rotated);
                });
                elevator.setBuildingId(getId());
                elevator.setAccessSides(sides);
                getGame().addIndustrialElevator(elevator);
            }
            board.getHex(coords).addTerrain(new Terrain(Terrains.INDUSTRIAL_ELEVATOR, elevator.getPlatformLevel()));
        }
        if (!getDesign().getElevators().isEmpty()) {
            gameManager.sendIndustrialElevatorUpdate();
        }

        // Send removed buildings to clients if any were replaced
        if (!removedBuildings.isEmpty()) {
            gameManager.sendRemovedBuildings(removedBuildings);
        }

        gameManager.sendNewBuildings(new Vector<>(List.of(this)));

        // Do this as a separate loop - All building terrains need added before we can initialize building exits
        for (Coords buildingCoords : getCoordsList()) {
            // Set up building exits to adjacent hexes with matching building type and class
            initializeBuildingExits(buildingCoords, boardId);

            // Notify clients of hex changes
            gameManager.sendChangedHex(buildingCoords, boardId);
        }
    }

    /**
     * Initializes building exits for a hex containing building terrain. This ensures that building hexes properly
     * connect to adjacent building hexes with matching building type and building class.
     *
     * @param buildingCoords the coordinates of the building hex
     * @param boardId        the board ID where the building is located
     */
    private void initializeBuildingExits(Coords buildingCoords, int boardId) {
        Hex hex = getGame().getBoard(boardId).getHex(buildingCoords);
        if (hex == null || !hex.containsTerrain(Terrains.BUILDING)) {
            return;
        }

        Terrain buildingTerrain = hex.getTerrain(Terrains.BUILDING);
        if (buildingTerrain == null) {
            return;
        }

        // Check each of the 6 directions
        for (int direction = 0; direction < 6; direction++) {
            Coords adjacentCoords = buildingCoords.translated(direction);
            Hex adjacentHex = getGame().getBoard(boardId).getHex(adjacentCoords);

            if (adjacentHex != null && adjacentHex.containsTerrain(Terrains.BUILDING)) {
                Terrain adjacentBuilding = adjacentHex.getTerrain(Terrains.BUILDING);

                // Buildings connect if they have the same building type (level)
                // and the same building class
                boolean sameType = (buildingTerrain.getLevel() == adjacentBuilding.getLevel());
                boolean sameClass = (hex.terrainLevel(Terrains.BLDG_CLASS)
                      == adjacentHex.terrainLevel(Terrains.BLDG_CLASS));

                // Gun emplacements never connect (single hex buildings)
                boolean isGunEmplacement = (hex.terrainLevel(Terrains.BLDG_CLASS) == IBuilding.GUN_EMPLACEMENT);

                buildingTerrain.setExit(direction, sameType && sameClass && !isGunEmplacement);
            } else {
                // No building adjacent in this direction
                buildingTerrain.setExit(direction, false);
            }
        }
    }


    @Override
    public void refreshLocations() {
        // We do not remove locations when the internal building removes a hex - we need to track the destroyed
        // locations!
        locationToRelativeCoordsMap.clear();
        if (!(getInternalBuilding() == null || getInternalBuilding().getOriginalCoordsList() == null)) {
            int location = 0;
            for (CubeCoords coords : getInternalBuilding().getOriginalCoordsList()) {
                for (int level = 0; level < getInternalBuilding().getBuildingHeight(); level++) {
                    locationToRelativeCoordsMap.put(location, coords);
                    location++;
                }
            }
        }
        super.refreshLocations();
    }

    /**
     *
     * @param coords              Board {@link Coords} that contain this building and are collapsing
     * @param numLevelsToCollapse number of floors to collapse, from the top
     */
    public void collapseFloorsOnHex(Coords coords, int numLevelsToCollapse) {
        if (numLevelsToCollapse <= 0) {
            return;
        }
        int startHexBuildingHeight = getHeight(coords);
        if (startHexBuildingHeight <= 0) {
            if (!isIn(coords)) {
                for (int floor = 0; floor < building.getBuildingHeight(); floor++) {
                    applyCollapseFloorLocationDamage(coords, floor);
                }
                updateRelativeLayout();
            }
            return;
        }
        for (int levelsRemoved = 1; levelsRemoved <= numLevelsToCollapse; levelsRemoved++) {
            applyCollapseFloorLocationDamage(coords, startHexBuildingHeight - levelsRemoved);
            setHeight(startHexBuildingHeight - levelsRemoved, coords);
            if (startHexBuildingHeight - levelsRemoved <= 0) {
                // Stop the for loop, hex is fully destroyed. If basements...
                // I don't think we deal with basements like that yet
                break;
            }
        }
        updateRelativeLayout();
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    private void applyCollapsedHexLocationDamage(Coords coords) {
        for (int floor = 0; floor < getInternalBuilding().getBuildingHeight(); floor++) {
            applyCollapseFloorLocationDamage(coords, floor);
        }
    }

    /**
     *
     */
    private void applyCollapseFloorLocationDamage(Coords coords, int floor) {
        for (int location : locationToRelativeCoordsMap.keySet()) {
            if (coords.equals(relativeToBoard(locationToRelativeCoordsMap.get(location)))) {
                if (location % getInternalBuilding().getBuildingHeight() == floor) {
                    setInternal(0, location);
                    destroyLocation(location, true);
                }
            }
        }
    }

    /** A {@link #crewCount} of this value means the crew is derived from the Advanced Building Minimum Crew Table. */
    public static final int CREW_FROM_MINIMUM_CREW_TABLE = -1;

    /** Gunners a capital-scale weapon needs (TO:AR p. 130). */
    private static final int GUNNERS_PER_CAPITAL_WEAPON = 7;
    /** Tons of heavy weapon one gunner serves (TO:AR p. 130): gunners are the weapon's tons divided by this, rounded up. */
    private static final double HEAVY_WEAPON_TONS_PER_GUNNER = 5.0;
    /** Non-gunners a field kitchen needs (TO:AR p. 130). */
    private static final int CREW_PER_FIELD_KITCHEN = 3;
    /** Non-gunners each MASH operating theater needs (TO:AR p. 130). */
    private static final int CREW_PER_MASH_THEATER = 5;
    /** Non-gunners a mobile field base needs (TO:AR p. 130). */
    private static final int CREW_PER_MOBILE_FIELD_BASE = 5;
    /** The largest non-officer crew that a single officer commands (TO:AR p. 130). */
    private static final int LARGEST_CREW_WITH_ONE_OFFICER = 9;
    /** Crew per officer once the crew is larger than {@link #LARGEST_CREW_WITH_ONE_OFFICER} (TO:AR p. 130). */
    private static final double CREW_PER_OFFICER = 10.0;

    /**
     * The crew this building was built with, from the {@code crew} block of its unit file, or
     * {@link #CREW_FROM_MINIMUM_CREW_TABLE} when the file has none and the crew is the table minimum.
     */
    private int crewCount = CREW_FROM_MINIMUM_CREW_TABLE;

    /**
     * Sets the crew this building was built with. The unit file's {@code crew} block sets it; a value of
     * {@link #CREW_FROM_MINIMUM_CREW_TABLE} returns to the Advanced Building Minimum Crew Table, and so does any
     * other negative value, since a negative crew is not a crew.
     *
     * @param crewCount the crew, or {@link #CREW_FROM_MINIMUM_CREW_TABLE}
     */
    public void setCrewCount(int crewCount) {
        this.crewCount = Math.max(crewCount, CREW_FROM_MINIMUM_CREW_TABLE);
    }

    /**
     * @return {@code true} when the crew comes from the unit file rather than the minimum crew table
     */
    public boolean hasExplicitCrewCount() {
        return crewCount != CREW_FROM_MINIMUM_CREW_TABLE;
    }

    /**
     * The building's crew: the unit file's {@code crew} block when it has one, otherwise the Advanced Building
     * Minimum Crew Table (TO:AR p. 130). Bay personnel are separate; see {@link #getBayPersonnel()}.
     *
     * @return the crew count
     */
    @Override
    public int getNCrew() {
        if (hasExplicitCrewCount()) {
            return crewCount;
        }
        return calculateMinimumCrew();
    }

    /**
     * The Advanced Building Minimum Crew Table (TO:AR p. 130): non-gunners for the equipment that needs them, one
     * gunner per light or medium weapon, one per five tons (rounded up) of each heavy weapon, seven per capital
     * weapon, and officers for the whole.
     *
     * @return the minimum crew for this building's equipment
     */
    public int calculateMinimumCrew() {
        return calculateMinimumCrewRequirements().total();
    }

    public record CrewRequirements(int crew, int gunners, int officers) {
        public int total() {
            return crew + gunners + officers;
        }
    }

    /** The same minimum crew breakdown used by construction, record sheets and the game. */
    public CrewRequirements calculateMinimumCrewRequirements() {
        int nonGunners = calculateBaseCrew() + calculateNonGunnerCrew();
        int gunners = calculateGunnerCrew();
        int officers = calculateOfficerCrew(nonGunners + gunners);
        return new CrewRequirements(nonGunners, gunners, officers);
    }

    /** Stationary structures need operators only; mobile structures also need a motive/control crew. */
    protected int calculateBaseCrew() {
        return 0;
    }

    /**
     * Equipment operators from the minimum crew table, including authored flight and landing facilities.
     */
    private int calculateNonGunnerCrew() {
        int nonGunners = 0;
        for (MiscMounted mounted : getMisc()) {
            MiscType miscType = mounted.getType();
            if (miscType.hasFlag(MiscType.F_COMMUNICATIONS)) {
                nonGunners += (int) Math.ceil(mounted.getTonnage());
            } else if (miscType.hasFlag(MiscType.F_FIELD_KITCHEN)) {
                nonGunners += CREW_PER_FIELD_KITCHEN;
            } else if (miscType.hasFlag(MiscType.F_MASH)) {
                nonGunners += CREW_PER_MASH_THEATER * Math.max(1, (int) mounted.getSize());
            } else if (miscType.hasFlag(MiscType.F_MOBILE_FIELD_BASE)) {
                nonGunners += CREW_PER_MOBILE_FIELD_BASE;
            } else if (miscType instanceof BuildingEquipmentType facility) {
                nonGunners += switch (facility.getFacility()) {
                    case FLIGHT_DECK -> 20;
                    case HELIPAD -> 5;
                    case LANDING_DECK -> 3 * (int) mounted.getSize();
                    case MODULAR_LINKAGE -> 4;
                    default -> 0;
                };
            }
        }
        return nonGunners;
    }

    /**
     * Gunners for the mounted weapons (TO:AR pp. 129 to 130). The table's light and medium weapons are the
     * conventional infantry weapons of the TechManual, and take one gunner each. Every weapon of 0.25 tons or more
     * that a Mek or vehicle can mount, a machine gun or a medium laser included, is a heavy weapon and takes one
     * gunner per five tons rounded up, which is one gunner up to five tons. A capital weapon takes seven.
     */
    private int calculateGunnerCrew() {
        int gunners = 0;
        for (WeaponMounted mounted : getWeaponList()) {
            WeaponType weaponType = mounted.getType();
            if (!requiresGunner(mounted)) {
                continue;
            }
            if (BuildingConstruction.isCapital(weaponType)) {
                gunners += GUNNERS_PER_CAPITAL_WEAPON;
            } else if (weaponType.hasFlag(WeaponType.F_INFANTRY)) {
                gunners += 1;
            } else {
                gunners += (int) Math.ceil(mounted.getTonnage() / HEAVY_WEAPON_TONS_PER_GUNNER);
            }
        }
        return gunners;
    }

    public boolean requiresGunner(WeaponMounted weapon) {
        return !weapon.isWeaponGroup() && BuildingConstruction.requiresGunner(weapon.getType())
              && !getDesign().getAutomatedWeapons().contains(weapon);
    }

    public boolean hasLivingGunnersAt(Coords coords) {
        return getWeaponsAt(coords).stream()
              .anyMatch(weapon -> requiresGunner(weapon) && !hasDeadGunners(weapon.getLocation()));
    }

    /**
     * Officers for a crew: none for an empty crew, one for up to nine, otherwise one per ten rounded up.
     *
     * @param nonOfficerCrew the non-gunners and gunners together
     */
    private int calculateOfficerCrew(int nonOfficerCrew) {
        if (this instanceof BuildingEntity staticBuilding && getBldgClass() != FORTRESS
              && getBldgClass() != GUN_EMPLACEMENT && getBldgClass() != CASTLE_BRIAN
              && !staticBuilding.getDesign().hasCivilianOfficers()) {
            return 0;
        }
        if (nonOfficerCrew == 0) {
            return 0;
        }
        if (nonOfficerCrew <= LARGEST_CREW_WITH_ONE_OFFICER) {
            return 1;
        }
        return (int) Math.ceil(nonOfficerCrew / CREW_PER_OFFICER);
    }

    @Override
    public boolean isBoardable() {
        return true;
    }

    // ========== Crew committed to an infantry action (TO:AR pp. 169 to 170) ==========

    /** Crew fighting in the action now: the building's Marine Points come from these. */
    private int committedCrew = 0;
    /** Crew ever committed to the running action, dead or alive: the to-hit penalty while committed comes from these. */
    private int committedCrewEver = 0;

    /**
     * @return the crew fighting in the infantry action now
     */
    public int getCommittedCrew() {
        // Crew killed by other means (a critical hit, the building's collapse) count as gone from the committed too
        return Math.max(0, Math.min(committedCrew, getCrew().getCurrentSize()));
    }

    /**
     * @return the crew not yet committed and still alive, available to commit
     */
    public int getCrewAvailableToCommit() {
        return Math.max(0, getCrew().getCurrentSize() - committedCrew);
    }

    /**
     * Commits more of the crew to the infantry action. Committing crew degrades the building: the Crew Casualties
     * Table turns the share of the crew ever committed into crew hits for as long as the action runs (TO:AR p. 170).
     *
     * @param requested the crew the defender commits this turn
     *
     * @return the crew actually committed, capped at those available
     */
    public int commitCrew(int requested) {
        int committed = Math.max(0, Math.min(requested, getCrewAvailableToCommit()));
        committedCrew += committed;
        committedCrewEver += committed;
        refreshCrewHits();
        return committed;
    }

    /**
     * Crew hits the building would carry with more crew committed, for the player deciding how many.
     *
     * @param additional the crew about to be committed
     *
     * @return the crew hits from the Crew Casualties Table for that share of the crew
     */
    public int getCrewHitsIfCommitted(int additional) {
        return Math.max(InfantryCombatTables.getCrewHits(percentOfCrew(committedCrewEver + additional)),
              getCrew().calculateHits());
    }

    /**
     * Removes casualties from the committed crew: they fall off the committed count and the crew itself.
     *
     * @param lost the crew lost
     */
    public void loseCommittedCrew(int lost) {
        int actualLoss = Math.max(0, Math.min(lost, committedCrew));
        committedCrew -= actualLoss;
        getCrew().setCurrentSize(Math.max(0, getCrew().getCurrentSize() - actualLoss));
        refreshCrewHits();
    }

    /** The action is over: nobody is committed, and only the crew actually lost still counts against the building. */
    public void clearCommittedCrew() {
        committedCrew = 0;
        committedCrewEver = 0;
        refreshCrewHits();
    }

    private int percentOfCrew(int people) {
        int crewSize = getCrew().getSize();
        return (crewSize <= 0) ? 0 : (int) Math.round(100.0 * people / crewSize);
    }

    private void refreshCrewHits() {
        int hits = getCrewHitsIfCommitted(0);
        boolean anyoneLeft = getCrew().getCurrentSize() > 0;
        for (int slot = 0; slot < getCrew().getSlotCount(); slot++) {
            getCrew().setHits(hits, slot);
            if (anyoneLeft) {
                // The Crew Casualties Table's hits are a weapon attack modifier (TO:AR p. 174), not a death: a crew
                // that commits everyone fights at +6 and still mans the building. Only losing them all kills them.
                getCrew().setDead(false, slot);
            }
        }
        if (!anyoneLeft) {
            getCrew().setDoomed(true);
        }
    }

    @Override
    public boolean canDeclareInfantryAction() {
        if ((game == null) || !game.hasBoardLocationOf(this)) {
            return false;
        }
        return InfantryActionStrengths.hasStake(game, this);
    }

    @Override
    public boolean canReinforceInfantryVsInfantry() {
        // AbstractBuildingEntity can reinforce if it's the target of ongoing combat
        return getGame().getEntitiesVector(getBoardLocation()).stream()
              .anyMatch(e -> e.getInfantryCombatTargetId() == this.getId());
    }

    // ========== Advanced Building Critical Damage (TO:AR pp. 118-119) ==========

    /** Turns the gunners remain stunned; a stunned building takes no actions (TO:AR p. 118, Gunners Stunned). */
    private int stunnedTurns = 0;

    /**
     * Whether a gamemaster has cut this structure's power at the switch. This is not a rules state; it is the
     * gamemaster's way of taking a building off line - a substation lost, a scenario event - without having to
     * destroy its generator. A structure switched off has no power however healthy its generators are.
     */
    private boolean powerSwitchedOff = false;

    /**
     * Locations whose gunners were killed by a critical hit; no weapon in them fires again (TO:AR p. 118). Not
     * final: a building deserialized from a save written before this field existed comes back with it {@code null},
     * so it is created on first use.
     */
    private Set<Integer> deadGunnerLocations = new HashSet<>();

    /**
     * Equipment numbers of turreted weapons locked in their current facing by a critical hit (TO:AR p. 118). Not
     * final for the same deserialization reason as {@link #deadGunnerLocations}.
     */
    private Set<Integer> lockedTurretWeapons = new HashSet<>();

    /** Per-hex gunner stuns; the scalar stunnedTurns remains the whole-building GM override. */
    private Map<Integer, Integer> stunnedGunnerLocations = new HashMap<>();
    private Set<Integer> pendingGunnerStunLocations = new HashSet<>();

    /** Advanced-building criticals use start-of-turn CF, not the absorption CF refreshed each phase. */
    private Map<CubeCoords, Integer> criticalStartCF = new HashMap<>();

    private Map<CubeCoords, Integer> criticalStartCF() {
        if (criticalStartCF == null) {
            criticalStartCF = new HashMap<>();
        }
        return criticalStartCF;
    }

    public int getCriticalDamageThreshold(Coords coords) {
        CubeCoords relative = boardToRelative(coords);
        int cf = criticalStartCF().computeIfAbsent(relative, key -> getCurrentCF(coords));
        return (int) Math.ceil(cf / 10.0);
    }

    private Set<Integer> pendingGunnerStunLocations() {
        if (pendingGunnerStunLocations == null) {
            pendingGunnerStunLocations = new HashSet<>();
        }
        return pendingGunnerStunLocations;
    }

    /** A second turret jam locks it even when the first jam has already been cleared (TO:AR p. 118). */
    private Set<Integer> previouslyJammedTurretWeapons = new HashSet<>();
    private Set<Integer> jammedTurretWeapons = new HashSet<>();
    private Set<Integer> repairedBuildingWeapons = new HashSet<>();

    private Set<Integer> jammedTurretWeapons() {
        if (jammedTurretWeapons == null) {
            jammedTurretWeapons = new HashSet<>();
        }
        return jammedTurretWeapons;
    }

    private Set<Integer> repairedBuildingWeapons() {
        if (repairedBuildingWeapons == null) {
            repairedBuildingWeapons = new HashSet<>();
        }
        return repairedBuildingWeapons;
    }

    private Map<Integer, Integer> stunnedGunnerLocations() {
        if (stunnedGunnerLocations == null) {
            stunnedGunnerLocations = new HashMap<>();
        }
        return stunnedGunnerLocations;
    }

    private Set<Integer> previouslyJammedTurretWeapons() {
        if (previouslyJammedTurretWeapons == null) {
            previouslyJammedTurretWeapons = new HashSet<>();
        }
        return previouslyJammedTurretWeapons;
    }

    private Set<Integer> deadGunnerLocations() {
        if (deadGunnerLocations == null) {
            deadGunnerLocations = new HashSet<>();
        }
        return deadGunnerLocations;
    }

    private Set<Integer> lockedTurretWeapons() {
        if (lockedTurretWeapons == null) {
            lockedTurretWeapons = new HashSet<>();
        }
        return lockedTurretWeapons;
    }

    /**
     * A building is never inside a building. Without this override the building entity is treated as a unit standing
     * inside its own hex, which makes weapon fire absorb against it once as a building and again as an occupant.
     *
     * @return {@code false}
     */
    @Override
    public boolean isInBuilding() {
        return false;
    }

    /**
     * A building's weapons face outward and its turreted weapons sit on the roof (TO:AR p. 132), so it cannot fire on
     * a unit inside one of its own hexes. A unit standing on the roof is outside the building and is not refused by
     * this rule; the ordinary zero-range rule still applies to it.
     *
     * @param unit the unit being targeted
     *
     * @return {@code true} if the unit occupies one of this building's hexes below roof level
     */
    public boolean isInsideThisBuilding(Entity unit) {
        boolean onMyBoard = unit.getBoardId() == getBoardId();
        boolean inOneOfMyHexes = (unit.getPosition() != null) && getCoordsList().contains(unit.getPosition());
        return onMyBoard && inOneOfMyHexes && BuildingRuntimeState.inside(this, unit);
    }

    /**
     * @param location an entity location (one hex level of this building)
     *
     * @return the board hex that location belongs to, or {@code null} if the location is unknown
     */
    public @Nullable Coords getLocationCoords(int location) {
        CubeCoords relativeCoords = locationToRelativeCoordsMap.get(location);
        return (relativeCoords == null) ? null : relativeToBoard(relativeCoords);
    }

    /**
     * @param location an entity location (one hex level of this building)
     *
     * @return the level within the hex that location represents; {@code 0} is the ground level
     */
    public int getLocationLevel(int location) {
        int buildingHeight = getInternalBuilding().getBuildingHeight();
        BuildingFloorState floors = building.getFloorState(locationToRelativeCoordsMap.get(location));
        if (floors != null && buildingHeight > 0) {
            return floors.getLevel(location % buildingHeight);
        }
        return (buildingHeight > 0) ? location % buildingHeight : 0;
    }

    /**
     * @param coords a board hex of this building, or {@code null}
     *
     * @return every entity location (one per level) that sits in that hex; empty when the hex is not part of this
     *       building
     */
    public List<Integer> getLocationsAt(@Nullable Coords coords) {
        return coordsToLocations(coords);
    }

    /**
     * @param coords a board hex of this building
     *
     * @return the weapons mounted in any level of that hex
     */
    public List<WeaponMounted> getWeaponsAt(Coords coords) {
        List<Integer> locations = getLocationsAt(coords);
        return getWeaponList().stream()
              .filter(weapon -> locations.contains(weapon.getLocation()))
              .toList();
    }

    /**
     * @param coords a board hex of this building
     *
     * @return the ammunition bins mounted in any level of that hex
     */
    public List<AmmoMounted> getAmmoAt(Coords coords) {
        List<Integer> locations = getLocationsAt(coords);
        return getAmmo().stream()
              .filter(ammo -> locations.contains(ammo.getLocation()))
              .toList();
    }

    /**
     * @param coords a board hex of this building
     *
     * @return the miscellaneous equipment mounted in any level of that hex
     */
    public List<MiscMounted> getMiscAt(Coords coords) {
        List<Integer> locations = getLocationsAt(coords);
        return getMisc().stream()
              .filter(misc -> locations.contains(misc.getLocation()))
              .toList();
    }

    /**
     * @return the number of turns the gunners remain stunned; {@code 0} when they can act
     */
    public int getStunnedTurns() {
        return Math.max(stunnedTurns, stunnedGunnerLocations().values().stream().mapToInt(Integer::intValue)
              .max().orElse(0));
    }

    /**
     * @return {@code true} while any of the building's gunners are stunned
     */
    public boolean isStunned() {
        return getStunnedTurns() > 0;
    }

    public boolean isGunnersStunned(int location) {
        return stunnedTurns > 0 || (stunnedGunnerLocations().getOrDefault(location, 0) > 0
              && !pendingGunnerStunLocations().contains(location));
    }

    public void stunGunnersAt(Coords coords) {
        stunGunnersAt(coords, -1);
    }

    public void stunGunnersAt(Coords coords, int level) {
        for (int location : getLocationsAt(coords)) {
            if (!hasDeadGunners(location) && (level < 0 || getLocationLevel(location) == level)) {
                if (!stunnedGunnerLocations().containsKey(location)) {
                    pendingGunnerStunLocations().add(location);
                }
                stunnedGunnerLocations().compute(location, (key, turns) -> turns == null ? 2 : turns + 1);
            }
        }
    }

    /**
     * The weapons jammed by a Weapon Malfunction critical hit (TO:AR p. 119) that gunners are still alive to clear.
     * A jammed weapon in a location whose gunners were killed stays jammed for good and is not listed.
     *
     * @return the jammed weapons with living gunners, empty when there are none
     */
    public List<Mounted<?>> getJammedWeapons() {
        List<Mounted<?>> jammedWeapons = new ArrayList<>();
        for (WeaponMounted weapon : getWeaponList()) {
            if ((weapon.isJammed() || isTurretJammed(weapon)) && requiresGunner(weapon)
                  && !hasDeadGunners(weapon.getLocation())) {
                jammedWeapons.add(weapon);
            }
        }
        return jammedWeapons;
    }

    /**
     * Whether the gunners can spend this turn clearing a jammed weapon, as a vehicle crew can (TW p. 195): a weapon
     * must be jammed, and the gunners must be neither stunned nor dead. Shared by the firing display and the server so
     * both sides agree.
     *
     * @return {@code true} when a Clear Weapon Jam action is available
     */
    public boolean canUnjamWeapon() {
        if (getJammedWeapons().isEmpty()) {
            return false;
        }
        if (getJammedWeapons().stream().allMatch(weapon -> isGunnersStunned(weapon.getLocation()))) {
            logger.debug("[WeaponJam] {}: cannot clear a jam, gunners stunned for {} more turns", getShortName(),
                  getStunnedTurns());
            return false;
        }
        if (allGunnersDead()) {
            logger.debug("[WeaponJam] {}: cannot clear a jam, all gunners are dead", getShortName());
            return false;
        }
        return true;
    }

    /** Repairing a malfunction silences its hex; a turret repair only silences that hex's turret. */
    private boolean repairBlocksWeapon(WeaponMounted repaired, WeaponMounted firing) {
        if (!Objects.equals(locationToRelativeCoordsMap.get(repaired.getLocation()),
              locationToRelativeCoordsMap.get(firing.getLocation()))) {
            return false;
        }
        if (usesExpandedCF() && getLocationLevel(repaired.getLocation()) != getLocationLevel(firing.getLocation())) {
            return false;
        }
        return !isTurretJammed(repaired) || isTurretMounted(firing);
    }

    public boolean isWeaponBlockedByRepair(WeaponMounted weapon) {
        if (repairedBuildingWeapons().stream().map(this::getWeapon)
              .filter(Objects::nonNull).anyMatch(repaired -> repairBlocksWeapon(repaired, weapon))) {
            return true;
        }
        return getGame() != null && getGame().getActionsVector().stream()
              .filter(action -> action.getEntityId() == getId())
              .filter(RepairWeaponMalfunctionAction.class::isInstance)
              .map(RepairWeaponMalfunctionAction.class::cast)
              .map(action -> getWeapon(action.getWeaponId()))
              .filter(Objects::nonNull)
              .anyMatch(repaired -> repairBlocksWeapon(repaired, weapon));
    }

    /** Called by the server once per accepted repair; repeat declarations cannot clear a second malfunction. */
    public boolean repairBuildingWeapon(WeaponMounted weapon) {
        if (!getJammedWeapons().contains(weapon) || isGunnersStunned(weapon.getLocation())
              || repairedBuildingWeapons().stream().map(this::getWeapon).filter(Objects::nonNull)
                    .anyMatch(repaired -> repairBlocksWeapon(repaired, weapon))) {
            return false;
        }
        repairedBuildingWeapons().add(getEquipmentNum(weapon));
        if (!isTurretJammed(weapon)) {
            weapon.setJammed(false);
        }
        // Keep the turret frozen and its repair restriction for the remainder of this phase/turn.
        return true;
    }

    /**
     * Applies a Gunners Stunned critical hit (TO:AR p. 118): the building takes no actions during the following turn.
     * Multiple stuns in the same turn extend the effect by one turn each, matching vehicle crew stuns.
     */
    public void stunGunners() {
        if (stunnedTurns == 0) {
            stunnedTurns = 2;
        } else {
            stunnedTurns++;
        }
    }

    /**
     * Applies a Gunners Killed critical hit (TO:AR p. 118) to one hex: no weapon in that hex fires for the rest of the
     * scenario. When every hex has lost its gunners the building's crew is marked as doomed.
     *
     * @param coords the board hex whose gunners were killed
     */
    public void killGunnersAt(Coords coords) {
        setGunnersKilledAt(coords, true);
    }

    /**
     * Sets or clears the Gunners Killed state of one hex. Killing is what a critical hit does; clearing exists so a
     * gamemaster can take the result back, which the rules themselves never do.
     *
     * @param coords the board hex whose gunners are being killed or restored
     * @param killed {@code true} to silence the hex, {@code false} to give it its gunners back
     */
    public void setGunnersKilledAt(Coords coords, boolean killed) {
        setGunnersKilled(getLocationsAt(coords), killed);
    }

    /**
     * Sets or clears the Gunners Killed state of the hex a location belongs to, without going out to the board and
     * back. A building that has not been placed yet has no board position to translate through, so this is the form
     * the editor uses: in the lobby the hex a location sits in is known but its board hex is not.
     *
     * @param location an entity location of the hex being silenced or restored
     * @param killed   {@code true} to silence the hex, {@code false} to give it its gunners back
     */
    public void setGunnersKilledAtLocation(int location, boolean killed) {
        setGunnersKilled(usesExpandedCF() ? List.of(location)
              : locationsForRelativeCoords(locationToRelativeCoordsMap.get(location)), killed);
    }

    /**
     * Sets or clears the Gunners Killed state of the given locations, and brings the crew's doomed flag with it.
     *
     * @param locations the entity locations to silence or restore
     * @param killed    {@code true} to silence them, {@code false} to give them their gunners back
     */
    private void setGunnersKilled(List<Integer> locations, boolean killed) {
        if (killed) {
            deadGunnerLocations().addAll(locations);
        } else {
            deadGunnerLocations().removeAll(locations);
        }
    }

    /**
     * Sets the number of turns the gunners remain stunned, overriding whatever a critical hit left. A gamemaster
     * uses this to stun a building or to bring it back to its senses; the rules themselves only ever add turns
     * through {@link #stunGunners()}.
     *
     * @param turns turns remaining, counted the way {@link #stunGunners()} sets them; negative is treated as none
     */
    public void setStunnedTurns(int turns) {
        if (turns == getStunnedTurns()) {
            return;
        }
        stunnedGunnerLocations().clear();
        pendingGunnerStunLocations().clear();
        stunnedTurns = Math.max(turns, 0);
    }

    /**
     * @return {@code true} when a gamemaster has cut this structure's power at the switch, which leaves it without
     *       power however healthy its generators are
     */
    public boolean isPowerSwitchedOff() {
        return powerSwitchedOff;
    }

    /**
     * Switches this structure's power on or off. Switching off takes it down as surely as losing its generator
     * does; switching back on only restores it if its generators can still carry the load.
     *
     * @param switchedOff {@code true} to cut the power, {@code false} to put it back on
     */
    public void setPowerSwitchedOff(boolean switchedOff) {
        powerSwitchedOff = switchedOff;
    }

    /**
     * @param location an entity location
     *
     * @return {@code true} if a Gunners Killed critical hit has silenced that location
     */
    public boolean hasDeadGunners(int location) {
        return deadGunnerLocations().contains(location);
    }

    /**
     * @return {@code true} when every location of this building has lost its gunners
     */
    public boolean allGunnersDead() {
        return !locationToRelativeCoordsMap.isEmpty()
              && deadGunnerLocations().containsAll(locationToRelativeCoordsMap.keySet());
    }

    /**
     * A building weapon is turret-mounted when its unit file marks it {@code (ST)} or {@code (PT)}; the loader stores
     * that as the Mek or pintle turret flag, so {@link Mounted#isTurret()} does not see it.
     *
     * @param weapon a weapon of this building
     *
     * @return {@code true} if the weapon sits in a turret and can normally fire in any direction
     */
    public boolean isTurretMounted(WeaponMounted weapon) {
        return weapon.isMekTurretMounted() || weapon.isPintleTurretMounted() || weapon.isSponsonTurretMounted();
    }

    /**
     * Applies a Turret Locks critical hit (TO:AR p. 118) to one turreted weapon: it keeps firing, but only into the
     * building's forward arc.
     *
     * @param weapon the turreted weapon to lock
     */
    public void lockTurretWeapon(WeaponMounted weapon) {
        setTurretLocked(weapon, true);
    }

    public void jamTurretWeapon(WeaponMounted weapon) {
        if (isTurretLocked(weapon)) {
            return;
        }
        if (previouslyJammedTurretWeapons().add(getEquipmentNum(weapon))) {
            jammedTurretWeapons().add(getEquipmentNum(weapon));
        } else {
            lockTurretWeapon(weapon);
        }
    }

    /**
     * Sets or clears the Turret Locks state of one turreted weapon. Locking is what a critical hit does; unlocking
     * exists so a gamemaster can take the result back, which the rules themselves never do.
     *
     * @param weapon the turreted weapon to lock or free
     * @param locked {@code true} to fix the weapon to the forward arc, {@code false} to give it its traverse back
     */
    public void setTurretLocked(WeaponMounted weapon, boolean locked) {
        jammedTurretWeapons().remove(getEquipmentNum(weapon));
        if (locked) {
            lockedTurretWeapons().add(getEquipmentNum(weapon));
        } else {
            lockedTurretWeapons().remove(getEquipmentNum(weapon));
            previouslyJammedTurretWeapons().remove(getEquipmentNum(weapon));
        }
    }

    /**
     * @param weapon a weapon of this building
     *
     * @return {@code true} if a Turret Locks critical hit has fixed that weapon's facing
     */
    public boolean isTurretLocked(WeaponMounted weapon) {
        return lockedTurretWeapons().contains(getEquipmentNum(weapon));
    }

    public boolean isTurretJammed(WeaponMounted weapon) {
        return jammedTurretWeapons().contains(getEquipmentNum(weapon));
    }

    /**
     * @return {@code true} if any turret of this building has been locked by a critical hit
     */
    public boolean hasLockedTurret() {
        return !lockedTurretWeapons().isEmpty();
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);
        building.newRound();
        getBuildingRuntimeState().newRound(this);
        for (int weaponId : repairedBuildingWeapons()) {
            WeaponMounted repaired = getWeapon(weaponId);
            if (repaired != null && isTurretJammed(repaired)) {
                getWeaponsAt(getLocationCoords(repaired.getLocation())).stream().filter(this::isTurretMounted)
                      .forEach(weapon -> jammedTurretWeapons().remove(getEquipmentNum(weapon)));
            }
        }
        repairedBuildingWeapons().clear();
        if (stunnedTurns > 0) {
            stunnedTurns--;
        }
        stunnedGunnerLocations().replaceAll((location, turns) -> turns - 1);
        stunnedGunnerLocations().values().removeIf(turns -> turns <= 0);
        pendingGunnerStunLocations().clear();
        criticalStartCF().clear();
        for (CubeCoords coords : building.getCoordsList()) {
            criticalStartCF().put(coords, building.getCurrentCF(coords));
        }
    }


    public BuildingDesign getDesign() {
        return design;
    }

    private BuildingRuntimeState buildingRuntimeState;

    public BuildingRuntimeState getBuildingRuntimeState() {
        if (buildingRuntimeState == null) {
            buildingRuntimeState = new BuildingRuntimeState();
        }
        return buildingRuntimeState;
    }


    @Override
    public boolean hasEnvironmentalSealing() {
        return getBldgClass() == IBuilding.CASTLE_BRIAN || design.hasEnvironmentalSealing();
    }


    /** TO:AR p. 127: Castles Brian store CF and armor in capital points. */
    public int getConstructionCFScale() {
        return getBldgClass() == IBuilding.CASTLE_BRIAN ? 10 : 1;
    }


    public double armorWeightInHex(CubeCoords hex) {
        int location = getInternalBuilding().getOriginalCoordsList().indexOf(hex) * getInternalBuilding().getBuildingHeight();
        return Math.ceil(getOArmor(location) * getConstructionCFScale() / (isClan() ? 20.0 : 16.0))
              * BuildingConstruction.segmentsInHex(this, hex);
    }


    /** Armor is purchased in whole tons per hex, once for the full height (TO:AR, p. 128). */
    @Override
    public double getArmorWeight() {
        return getInternalBuilding().getOriginalCoordsList().stream().mapToDouble(this::armorWeightInHex).sum();
    }


    public List<Mounted<?>> getEquipmentInHex(CubeCoords hex) {
        int index = getInternalBuilding().getOriginalCoordsList().indexOf(hex);
        if (index < 0) {
            return List.of();
        }
        return getEquipment().stream().filter(m -> !m.isOneShotAmmo() && !m.isWeaponGroup()
              && BuildingConstruction.equipmentPositions(this, m).stream().anyMatch(p -> p.hex().equals(hex))).toList();
    }


    public boolean hasFusionOrFissionPower() {
        return getEquipment().stream().anyMatch(m -> m.getType() instanceof PowerGeneratorType generator
              && (generator.getStructureEngine() == StructureEngine.FUSION
                    || generator.getStructureEngine() == StructureEngine.FISSION));
    }

    public abstract boolean hasPower();


    /** Ten percent per hex; static buildings round to 0.1 tons, mobile structures to 0.5 (TO:AR p. 129; TO:AUE p. 83). */
    public double getPowerAmplifierWeight(CubeCoords hex) {
        if (hasFusionOrFissionPower()) {
            return 0;
        }
        double energyWeapons = getEquipmentInHex(hex).stream().filter(m -> m.getType() instanceof WeaponType weapon
              && weapon.hasFlag(WeaponType.F_ENERGY) && !(weapon instanceof InfantryWeapon)
              && m.getTonnage() >= (this instanceof MobileStructure ? .5 : .25))
              .mapToDouble(m -> BuildingConstruction.equipmentWeightInHex(this, m, hex)).sum();
        return this instanceof MobileStructure ? Math.ceil(energyWeapons / 5) / 2 : Math.ceil(energyWeapons) / 10;
    }


    /** Roof turret and pintle mechanisms are derived from their mounted weapons (TO:AUE p. 83). */
    public double getTurretWeight(CubeCoords hex) {
        List<Mounted<?>> equipment = getEquipmentInHex(hex).stream()
              .filter(m -> !(m.getType() instanceof AmmoType) && !m.getType().hasFlag(MiscType.F_HEAT_SINK)
                    && !m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)).toList();
        double turret = equipment.stream().filter(Mounted::isSponsonTurretMounted).mapToDouble(Mounted::getTonnage).sum();
        return Math.ceil(turret / 5) / 2;
    }


    public double getPintleWeight(CubeCoords hex) {
        return getEquipmentInHex(hex).stream().filter(m -> m.isPintleTurretMounted() && !(m.getType() instanceof AmmoType))
              .mapToDouble(m -> Math.ceil(m.getTonnage() * 50) / 1000).sum();
    }


    @Override
    public boolean isBuildingEntityOrGunEmplacement() {
        return true;
    }


    /**
     * Calculates the base generator weight for an advanced building.
     * <p>
     * To find the Base Generator Weight for an advanced building (or a complex of buildings): 1. Add up the total
     * number of hexes for all advanced buildings intended to receive power 2. Exclude Tent-, Fence-, Wall- and
     * Bridge-class buildings 3. For multi-level buildings: multiply the building's hex-count by its height in levels
     * (plus any basement levels) before adding it to the sum 4. Add to this sum 10 percent of the total tonnage for all
     * Heavy-class energy weapons used by any of these buildings
     *
     * @return The base generator weight in tons
     */
    public double getBaseGeneratorWeight() {
        if (getInternalBuilding() == null) {
            return 0.0;
        }
        if (getBuildingType() == BuildingType.WALL || BuildingConstruction.hasNoInterior(this)
              || BuildingConstruction.usesHexsides(this)
              || BuildingConstruction.isLiquidStorageOnly(this)) {
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
                if (weaponType.hasFlag(WeaponType.F_ENERGY) && !(weaponType instanceof InfantryWeapon)) {
                    energyWeaponTonnage += equipment.getTonnage();
                }
            }
        }

        // Base Generator Weight = base hex weight + 10% of energy weapon tonnage
        return baseHexWeight + (energyWeaponTonnage * 0.1);
    }


    /**
     * Calculates the internal weight capacity for this building.
     * <p>
     * For each hex of area covered, advanced buildings may internally carry a total tonnage of equipment equal to their
     * Construction Factor times the number of levels of structure height.
     * <p>
     * Hangar-type structures may triple this capacity, but are limited to a maximum of 600 tons per hex for every 4
     * levels of structural height (or fraction thereof).
     *
     * @return The total internal weight capacity in tons
     */
    @Override
    public double getWeight() {
        Building building = getInternalBuilding();
        if (building == null) {
            return 0.0;
        }

        double total = building.getCoordsList().stream().mapToDouble(hex -> BuildingConstruction.capacityInHex(this, hex)).sum();
        return design.isOpenSpace() ? Math.min(design.hasHeavyMetal() ? 450 : 600, total) : total;
    }
}
