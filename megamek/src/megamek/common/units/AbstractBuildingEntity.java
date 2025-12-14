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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.cost.CostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.rolls.PilotingRollData;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * AbstractBuildingEntity represents a non-terrain building (e.g., a moving fortress).
 * This is the common implementation of the Mobile Structure rules from TO:AUE and Advanced Building rules from TO:AUE.
 * <br>
 * It contains a {@link Building} (which stores data in relative coordinates) and handles
 * translation between board coordinates and the Building's relative coordinate space.
 * Unlike BuildingTerrain, the translation is dynamic based on Entity's current position/facing.
 */
public abstract class AbstractBuildingEntity extends Entity implements IBuilding {

    private static final MMLogger logger = MMLogger.create(AbstractBuildingEntity.class);

    private final Building building;
    /**
     * Relative {@link CubeCoords} -> actual board {@link Coords}
     */
    private final Map<CubeCoords, Coords> relativeLayout = new HashMap<>();
    /**
     *  Entity location -> relative {@link CubeCoords}
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

    @Override
    public CubeCoords boardToRelative(Coords boardCoords) {
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

    @Override
    /**
     * TODO: Duplicate of subclass {@link BuildingEntity}, remove one
     */
    public int getWeaponFiringHeight(WeaponMounted weapon) {
        if (weapon == null || getInternalBuilding() == null) {
            return super.getWeaponFiringHeight(weapon);
        }
        int location = weapon.getLocation();
        // Extract the level from the location number (location % building height)
        // Level 0 = ground floor, level 1 = first floor above ground, etc.
        int level = location % getInternalBuilding().getBuildingHeight();
        return level;
    }

    /**
     * Override setPosition to populate the relativeLayout map when the entity is placed.
     * This establishes the mapping between the building's internal relative coordinates
     * and their actual board coordinates.
     */
    @Override
    public void setPosition(Coords position) {
        super.setPosition(position);
        updateRelativeLayout();
    }

    @Override
    public void setPosition(Coords position, boolean gameUpdate) {
        super.setPosition(position, gameUpdate);
        updateRelativeLayout();
    }

    /**
     * Returns true when the given location cannot legally be entered or deployed into by this unit at the given
     * elevation or altitude. Also returns true when the location doesn't exist. Even when this method returns true, the
     * location need not be deadly to the unit.
     *
     * @param testPosition  The position to test
     * @param testBoardId   The board to test
     * @param testElevation The elevation or altitude to test
     * @return True when the location is illegal to be in for this unit, regardless of elevation
     * @see #isLocationDeadly(Coords)
     */
    @Override
    public boolean isLocationProhibited(Coords testPosition, int testBoardId, int testElevation) {
        if (!game.hasBoardLocation(testPosition, testBoardId)) {
            return true;
        }

        Hex hex = game.getHex(testPosition, testBoardId);
        if (testElevation != 0) {
            return hex.containsTerrain(Terrains.IMPASSABLE);
        }

        // Check prohibited terrain
        boolean isProhibited = false;
        // Check for other entities
        var currentEntitiesIter = game.getEntities(testPosition);
        while (currentEntitiesIter.hasNext()) {
            Entity entity = currentEntitiesIter.next();
            isProhibited = isProhibited || !this.equals(entity);
            if (isProhibited) {
                return true;
            }
        }

        // Check for other buildings - we can't replace it if it's another AbstractBuildingEntity
        Optional<IBuilding> otherBuilding = game.getBuildingAt(testPosition, getBoardId());
        if (otherBuilding.isPresent() && !equals(otherBuilding.get()) && (otherBuilding.get() instanceof AbstractBuildingEntity)) {
            return true;
        }

        boolean noInvalidPositions = true;
        int elevation = hex.getLevel();
        List<CubeCoords> allPositions = getInternalBuilding().getCoordsList();
        for (CubeCoords cubeCoords : allPositions) {
            boolean invalidPosition = false;
            Coords secondaryCoords = testPosition.toCube().add(rotateCoordByFacing(cubeCoords, getFacing())).toOffset();
            Hex secondaryHex = game.getBoard(testBoardId).getHex(secondaryCoords);
            currentEntitiesIter = game.getEntities(secondaryCoords);
            boolean secondaryHexPresent = secondaryHex != null;
            // TODO: Secondary hex present for Mobile Structures has some kind of nuance
            isProhibited = isProhibited || !secondaryHexPresent;
            while (!isProhibited && currentEntitiesIter.hasNext()) {
                Entity entity = currentEntitiesIter.next();
                invalidPosition |= !this.equals(entity);
            }

            // Check for other buildings - we can't replace it if it's another AbstractBuildingEntity
            Optional<IBuilding> otherBuildingSecondary = game.getBuildingAt(testPosition, getBoardId());
            if (otherBuildingSecondary.isPresent() && !equals(otherBuildingSecondary.get()) && (otherBuildingSecondary.get() instanceof AbstractBuildingEntity)) {
                return true;
            }

            if (secondaryHexPresent) {
                invalidPosition |= secondaryHex.getLevel() != elevation;
            }
            if (invalidPosition) {
                noInvalidPositions = false;
            }
        }

        return isProhibited || !noInvalidPositions;
    }

    /**
     * Rotates a cube coordinate clockwise around the origin by the given facing.
     * Facing 0 is UP (no rotation), and each facing increment is 60° clockwise.
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
     * Computes what the relative layout would be for a hypothetical position and facing
     * WITHOUT modifying the entity's actual position or facing.
     * This is a pure calculation method with no side effects.
     *
     * @param testPosition The position to test
     * @param testFacing The facing to test (0-5)
     * @return Map of relative CubeCoords to their board Coords at the given position/facing
     */
    public Map<CubeCoords, Coords> computeLayoutForPositionAndFacing(Coords testPosition, int testFacing) {
        Map<CubeCoords, Coords> hypotheticalLayout = new HashMap<>();

        if (testPosition == null || building == null) {
            return hypotheticalLayout;
        }

        // Add origin
        hypotheticalLayout.put(CubeCoords.ZERO, testPosition);

        // Map each relative CubeCoord to its hypothetical board coordinate
        for (CubeCoords relCoord : building.getCoordsList()) {
            if (!relCoord.equals(CubeCoords.ZERO)) {
                // Rotate by the TEST facing, not the entity's actual facing
                CubeCoords rotatedRelCoord = rotateCoordByFacing(relCoord, testFacing);
                CubeCoords positionCubeCoords = testPosition.toCube();
                Coords boardCoord = positionCubeCoords.add(rotatedRelCoord).toOffset();

                hypotheticalLayout.put(relCoord, boardCoord);
            }
        }

        return hypotheticalLayout;
    }

    /**
     * Checks if all hexes of this building would be valid at the given position and facing
     * WITHOUT modifying the entity's state. This is a pure calculation method.
     *
     * @param testPosition The position to test
     * @param testFacing The facing to test (0-5)
     * @param testElevation The elevation to test
     * @param testBoardId The board ID to test
     * @return true if all building hexes would be valid at this position/facing
     */
    public boolean isPositionAndFacingValid(Coords testPosition, int testFacing, int testElevation, int testBoardId) {
        if (!game.hasBoardLocation(testPosition, testBoardId)) {
            return false;
        }

        Map<CubeCoords, Coords> hypotheticalLayout = computeLayoutForPositionAndFacing(testPosition, testFacing);
        Board board = game.getBoard(testBoardId);

        // All hexes must be at the same elevation
        Hex originHex = board.getHex(testPosition);
        if (originHex == null) {
            return false;
        }
        int requiredElevation = originHex.getLevel();

        // Check each hex in the hypothetical layout
        for (Coords boardCoord : hypotheticalLayout.values()) {
            if (!game.hasBoardLocation(boardCoord, testBoardId)) {
                return false;
            }

            Hex hex = board.getHex(boardCoord);
            if (hex == null) {
                return false;
            }

            // Check elevation consistency
            if (hex.getLevel() != requiredElevation) {
                return false;
            }

            // Check impassable terrain at non-ground elevations
            if (testElevation != 0 && hex.containsTerrain(Terrains.IMPASSABLE)) {
                return false;
            }

            // Check for other entities at this hex
            var entitiesAtHex = game.getEntities(boardCoord);
            while (entitiesAtHex.hasNext()) {
                Entity otherEntity = entitiesAtHex.next();
                if (!this.equals(otherEntity)) {
                    return false; // Another entity is blocking
                }
            }

            // Check for other buildings - we can't replace it if it's another AbstractBuildingEntity
            Optional<IBuilding> otherBuilding = game.getBuildingAt(boardCoord, getBoardId());
            if (otherBuilding.isPresent() && !equals(otherBuilding.get()) && (otherBuilding.get() instanceof AbstractBuildingEntity)) {
                return false;
            }

            // Check stacking violations
            if (Compute.stackingViolation(game, this, testElevation, boardCoord,
                  testBoardId, null, climbMode(), true) != null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns all valid facings for this building at the given position and elevation.
     * Does NOT modify the entity's facing - this is a pure calculation method.
     *
     * @param testPosition The position to test
     * @param testElevation The elevation to test
     * @param testBoardId The board ID to test
     * @return List of valid facings (0-5), or empty list if none valid
     */
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
     * Updates the relativeLayout map to reflect the current building configuration.
     * Maps each relative CubeCoord in the building to its actual board position.
     */
    private void updateRelativeLayout() {
        relativeLayout.clear();

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
     * @param facing
     */
    @Override
    public void setFacing(int facing) {
        super.setFacing(facing);
        updateRelativeLayout();
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
     * @param dir
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        return false;
    }

    /**
     * Returns the closest valid secondary facing to the given direction.
     *
     * @param dir
     *
     * @return the closest valid secondary facing.
     */
    @Override
    public int clipSecondaryFacing(int dir) {
        return 0;
    }

    @Override
    public String[] getLocationNames() {
        return getLocationStrings(LOCATION_NAMES_PREFIX);
    }

    @Override
    public String[] getLocationAbbreviations() {
        return getLocationStrings(LOCATION_ABBREVIATIONS_PREFIX);
    }

    private String[] getLocationStrings(String locationPrefix) {
        ArrayList<String> locationAbbrvNames = new ArrayList<String>();
        if (getInternalBuilding() == null || getInternalBuilding().getOriginalCoordsList() == null) {
            return new String[] { locationPrefix + ' ' + LOC_BASE };
        }
        for (int location : locationToRelativeCoordsMap.keySet()) {
            CubeCoords cubeCoords = locationToRelativeCoordsMap.get(location);
            String coordString;
            if (getPosition() == null) {
                coordString = cubeCoords.q() + "," + cubeCoords.r() + "," + cubeCoords.s();
            } else {
                CubeCoords positionCubeCoords = getPosition().toCube();
                coordString = positionCubeCoords.add(cubeCoords).toOffset().getBoardNum();
            }

            // Result is 0 indexed
            int level = (location % getInternalBuilding().getBuildingHeight());
            locationAbbrvNames.add(locationPrefix + ' ' + level + ' ' + coordString);
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
     * @param table
     * @param side
     * @param aimedLocation
     * @param aimingMode
     * @param cover
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        return rollHitLocation(table, side);
    }

    /**
     * Rolls up a hit location
     *
     * @param table
     * @param side
     */
    @Override
    public HitData rollHitLocation(int table, int side) {
        return new HitData(LOC_BASE, false, HitData.EFFECT_NONE);
    }

    /**
     * Gets the location that excess damage transfers to. That is, one location inwards.
     *
     * @param hit
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
        return 0;
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc. If false, assume it fires into the primary.
     *
     * @param weaponId
     */
    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        return false;
    }

    @Override
    public int[] getNoOfSlots() {
        return CRITICAL_SLOTS;
    }

    @Override
    public void addEquipment(Mounted<?> mounted, int loc, boolean rearMounted)
          throws LocationFullException {
        super.addEquipment(mounted, loc, rearMounted);
        // Add the piece equipment to our slots.
        addCritical(loc, new CriticalSlot(mounted));
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
        return null;
    }

    /**
     * Add in any piloting skill mods
     *
     * @param roll
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

    /**
     * Calculates and returns the C-bill cost of the unit. The parameter ignoreAmmo can be used to include or exclude
     * ("dry cost") the cost of ammunition on the unit. A report for the cost calculation will be written to the given
     * calcReport.
     *
     * @param calcReport A CalculationReport to write the report for the cost calculation to
     * @param ignoreAmmo When true, the cost of ammo on the unit will be excluded from the cost
     *
     * @return The cost in C-Bills of the 'Mek in question.
     */
    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        CostCalculator.addNoReportNote(calcReport, this);
        return 0;
    }

    /**
     * Checks if the unit is hardened against nuclear strikes.
     *
     * @return true if this is a hardened unit.
     */
    @Override
    public boolean isNuclearHardened() {
        return false;
    }

    @Override
    public boolean doomedInVacuum() {
        return false;
    }

    /**
     * @return the total tonnage of communications gear in this entity
     */
    @Override
    public int getTotalCommGearTons() {
        return getExtraCommGearTons();
    }

    @Override
    public int getEngineHits() {
        return 0;
    }

    @Override
    public String getLocationDamage(int loc) {
        return "";
    }

    /**
     * Returns TRUE if the entity meets the requirements for crippling damage as detailed in TW pg 258.
     *
     * @return boolean
     */
    @Override
    public boolean isCrippled() {
        return false;
    }

    /**
     * Returns TRUE if the entity meets the requirements for crippling damage as detailed in TW pg 258. Excepting dead
     * or non-existing crew issues
     *
     * @param checkCrew
     *
     * @return boolean
     */
    @Override
    public boolean isCrippled(boolean checkCrew) {
        return false;
    }

    /**
     * Returns TRUE if the entity has been heavily damaged.
     *
     * @return boolean
     */
    @Override
    public boolean isDmgHeavy() {
        return false;
    }

    /**
     * Returns TRUE if the entity has been moderately damaged.
     *
     * @return boolean
     */
    @Override
    public boolean isDmgModerate() {
        return false;
    }

    /**
     * Returns TRUE if the entity has been lightly damaged.
     *
     * @return boolean
     */
    @Override
    public boolean isDmgLight() {
        return false;
    }

    @Override
    public int getArmorType(int loc) {
        return 0;
    }

    @Override
    public int getArmorTechLevel(int loc) {
        return TechConstants.T_INTRO_BOX_SET;
    }

    @Override
    public boolean hasStealth() {
        return false;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_BUILDING_ENTITY;
    }

    /**
     * Returns the amount of armor in the location specified, or IArmorState.ARMOR_NA, or IArmorState.ARMOR_DESTROYED.
     *
     * @param loc
     * @param rear
     */
    @Override
    public int getArmor(int loc, boolean rear) {
        return IArmorState.ARMOR_NA;
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
        building.setPhaseCF(cf, boardToRelative(coords));
    }

    @Override
    public void setArmor(int a, Coords coords) {
        building.setArmor(a, boardToRelative(coords));
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

    @Override
    public void addDemolitionCharge(int playerId, int damage, Coords pos) {
        building.addDemolitionCharge(playerId, damage, boardToRelative(pos));
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
     * @param boardId
     * @param gameManager
     */
    public void updateBuildingEntityHexes(int boardId, TWGameManager gameManager) {
        Board board = getGame().getBoard(boardId);
        for (Coords buildingCoords : getCoordsList()) {
            Hex targetHex = board.getHex(buildingCoords);
            if (targetHex != null) {
                // Add building terrain with the building type
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
                int height = getHeight(buildingCoords);
                targetHex.addTerrain(new Terrain(Terrains.BLDG_ELEV, height));

                // Add basement type if present
                if (getBasement(buildingCoords) != null) {
                    targetHex.addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE,
                          getBasement(buildingCoords).ordinal()));
                }
            }
        }

        board.addBuildingToBoard(this);

        gameManager.sendNewBuildings(new Vector<IBuilding>(List.of(this)));

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

                if (sameType && sameClass && !isGunEmplacement) {
                    buildingTerrain.setExit(direction, true);
                } else {
                    buildingTerrain.setExit(direction, false);
                }
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
     * @param coords Board {@link Coords} that contain this building and are collapsing
     * @param numLevelsToCollapse number of floors to collapse, from the top
     */
    public void collapseFloorsOnHex(Coords coords, int numLevelsToCollapse) {
        if (numLevelsToCollapse <= 0) {
            return;
        }
        int startHexBuildingHeight = getHeight(coords);
        if (startHexBuildingHeight <= 0) {
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

    private void applyCollapsedHexLocationDamage(Coords coords) {
        for (int floor = 0; floor < getInternalBuilding().getBuildingHeight(); floor++) {
            applyCollapseFloorLocationDamage(coords, floor);
        }
    }

    /**
     *
     * @param coords
     * @param floor
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
}
