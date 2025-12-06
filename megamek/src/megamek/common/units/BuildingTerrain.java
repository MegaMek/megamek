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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.CardinalDirection;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.logging.MMLogger;

/**
 * BuildingTerrain represents a stationary building placed on the board.
 * <p>
 * It contains a Building (which stores data in relative coordinates) and handles translation between board coordinates
 * and the Building's relative coordinate space.
 */
public class BuildingTerrain implements IBuilding {
    private static final MMLogger logger = MMLogger.create(BuildingTerrain.class);

    private final Building building;
    private final Coords boardOrigin;  // Where relative (0,0,0) is in board coords
    private final int facing;          // Rotation: 0-5 (0 = north, clockwise)
    private final Map<Coords, CubeCoords> boardToRelativeMap = new HashMap<>();  // Board Coords -> Relative CubeCoords
    private final Map<CubeCoords, Coords> relativeToBoardMap = new HashMap<>();  // Relative CubeCoords -> Board Coords

    /**
     * Construct a BuildingTerrain from board information. Reads the building from the board and creates internal
     * Building with relative coords.
     */
    public BuildingTerrain(Coords coords, Board board, int structureType, BasementType basementType) {
        this.boardOrigin = coords;
        this.facing = 0;  // Buildings on board are not rotated

        // Get hex and validate
        Hex startHex = board.getHex(coords);
        if (!startHex.containsTerrain(structureType)) {
            throw new IllegalArgumentException("The coordinates, "
                  + coords.getBoardNum()
                  + ", do not contain a building.");
        }

        BuildingType type = BuildingType.getType(startHex.terrainLevel(structureType));
        int bldgClass = startHex.terrainLevel(Terrains.BLDG_CLASS);
        int id = IBuilding.currentId(board, coords);

        // Create Building with relative coordinates
        building = new Building(type, bldgClass, id, structureType);

        // Set building height from BLDG_ELEV
        int buildingHeight = startHex.containsTerrain(Terrains.BLDG_ELEV) ? startHex.terrainLevel(Terrains.BLDG_ELEV) : 0;
        building.setBuildingHeight(buildingHeight);

        // Recursively scan and add hexes starting from relative (0,0,0)
        scanAndAddHexes(coords, CubeCoords.ZERO, board, structureType, basementType);
    }

    /**
     * Recursively scan the board and add building hexes.
     *
     * @param boardCoords    the board coordinates to scan
     * @param relativeCoords the corresponding relative coordinates in CubeCoords
     * @param board          the game board
     * @param structureType  the terrain type
     * @param basementType   the basement type
     */
    private void scanAndAddHexes(Coords boardCoords, CubeCoords relativeCoords, Board board, int structureType,
          BasementType basementType) {
        // If already added, skip
        if (boardToRelativeMap.containsKey(boardCoords)) {
            return;
        }

        Hex hex = board.getHex(boardCoords);
        if (hex == null || !hex.containsTerrain(structureType)) {
            return;
        }

        // Add to mapping
        boardToRelativeMap.put(boardCoords, relativeCoords);
        relativeToBoardMap.put(relativeCoords, boardCoords);

        // Extract hex data
        int cf = IBuilding.getDefaultCF(building.getBuildingType());
        if (structureType == Terrains.BUILDING && hex.containsTerrain(Terrains.BLDG_CF)) {
            cf = hex.terrainLevel(Terrains.BLDG_CF);
        } else if (structureType == Terrains.BRIDGE && hex.containsTerrain(Terrains.BRIDGE_CF)) {
            cf = hex.terrainLevel(Terrains.BRIDGE_CF);
        } else if (structureType == Terrains.FUEL_TANK && hex.containsTerrain(Terrains.FUEL_TANK_CF)) {
            cf = hex.terrainLevel(Terrains.FUEL_TANK_CF);
        }

        int armor = hex.containsTerrain(Terrains.BLDG_ARMOR) ? hex.terrainLevel(Terrains.BLDG_ARMOR) : 0;
        BasementType basement = BasementType.getType(hex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE));
        boolean collapsed = hex.terrainLevel(Terrains.BLDG_BASE_COLLAPSED) == 1;

        // Add hex to building with CubeCoords
        building.addHex(relativeCoords, cf, armor, basement, collapsed);

        // Scan adjacent hexes
        for (int dir = 0; dir < 6; dir++) {
            if (hex.containsTerrainExit(structureType, dir)) {
                Coords nextBoard = boardCoords.translated(dir);
                CubeCoords nextRelative = relativeCoords.neighbor(CardinalDirection.values()[dir]);
                scanAndAddHexes(nextBoard, nextRelative, board, structureType, basementType);
            }
        }
    }

    @Override
    public Coords getBoardOrigin() {
        return boardOrigin;
    }

    @Override
    public int getBoardFacing() {
        return facing;
    }

    @Override
    public Building getInternalBuilding() {
        return building;
    }

    @Override
    public CubeCoords boardToRelative(Coords boardCoords) {
        return boardToRelativeMap.get(boardCoords);
    }

    @Override
    public Coords relativeToBoard(CubeCoords relativeCoords) {
        return relativeToBoardMap.get(relativeCoords);
    }

    /**
     * Get the ID of this building. The same ID applies to all hexes.
     *
     * @return the <code>int</code> ID of the building.
     */
    @Override
    public int getId() {
        return building.getId();
    }

    /**
     * Determines if the coord exist in the currentCF has.
     *
     * @param coords - the BOARD <code>Coords</code> being examined.
     *
     * @return <code>true</code> if the building has CF at the coordinates.
     *       <code>false</code> otherwise.
     */
    @Override
    public boolean hasCFIn(Coords coords) {
        CubeCoords relative = boardToRelative(coords);
        return relative != null && building.hasCFIn(relative);
    }

    /**
     * Get the BOARD coordinates that the building occupies.
     *
     * @return an <code>Enumeration</code> of the BOARD <code>Coord</code> objects.
     */
    @Override
    public Enumeration<Coords> getCoords() {
        // Return board coords, not relative
        return new Vector<>(relativeToBoardMap.values()).elements();
    }

    /**
     * Returns a list of this Building's BOARD coords. The list is unmodifiable.
     */
    @Override
    public List<Coords> getCoordsList() {
        // Return board coords, not relative
        return List.copyOf(relativeToBoardMap.values());
    }

    /**
     * Get the construction type of the building. This value will be one of the values defined in
     * megamek.common.enums.BuildingType
     *
     * @return the <code>int</code> code of the building's construction type.
     */
    @Override
    public BuildingType getBuildingType() {
        return building.getBuildingType();
    }

    /**
     * Get the building class, per TacOps rules.
     *
     * @return the <code>int</code> code of the building's classification.
     */
    @Override
    public int getBldgClass() {
        return building.getBldgClass();
    }

    /**
     * Get the building basement, per TacOps rules.
     *
     * @param coords BOARD coordinates
     *
     * @return the <code>int</code> code of the building basement type.
     */
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

    /**
     * Roll what kind of basement this building has
     *
     * @param coords       BOARD coordinates of the building to roll for
     * @param board        the game board
     * @param vPhaseReport the {@link Report} <code>Vector</code> containing the phase report
     *
     * @return a <code>boolean</code> indicating weather the hex and building was changed or not
     */
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

    /**
     * Get the current construction factor of the building hex at the passed coords. Any damage immediately updates this
     * value.
     *
     * @param coords the BOARD <code>Coords</code> of the hex in question
     *
     * @return the <code>int</code> value of the building hex's current construction factor. This value will be greater
     *       than or equal to zero.
     */
    @Override
    public int getCurrentCF(Coords coords) {
        return building.getCurrentCF(boardToRelative(coords));
    }

    /**
     * Get the construction factor of the building hex at the passed coords at the start of the current phase. Damage
     * that is received during the phase is applied at the end of the phase.
     *
     * @param coords the BOARD <code>Coords</code> of the hex in question
     *
     * @return the <code>int</code> value of the building's construction factor at the start of this phase. This value
     *       will be greater than or equal to zero.
     */
    @Override
    public int getPhaseCF(Coords coords) {
        return building.getPhaseCF(boardToRelative(coords));
    }

    @Override
    public int getArmor(Coords coords) {
        return building.getArmor(boardToRelative(coords));
    }

    /**
     * Set the current construction factor of the building hex. Call this method immediately when the building sustains
     * any damage.
     *
     * @param cf     the <code>int</code> value of the building hex's current construction factor. This value must be
     *               greater than or equal to zero.
     * @param coords the BOARD <code>Coords</code> of the hex in question
     *
     * @throws IllegalArgumentException if the passed value is less than zero
     */
    @Override
    public void setCurrentCF(int cf, Coords coords) {
        building.setCurrentCF(cf, boardToRelative(coords));
    }

    /**
     * Set the construction factor of the building hex for the start of the next phase. Call this method at the end of
     * the phase to apply damage sustained by the building during the phase.
     *
     * @param cf     the <code>int</code> value of the building hex's current construction factor. This value must be
     *               greater than or equal to zero.
     * @param coords the BOARD <code>Coords</code> of the hex in question
     *
     * @throws IllegalArgumentException if the passed value is less than zero
     */
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

    /**
     * Get the name of this building.
     *
     * @return the <code>String</code> name of this building.
     */
    @Override
    public String getName() {
        return building.getName();
    }

    /**
     * Determine if this building is on fire.
     *
     * @param coords BOARD coordinates
     *
     * @return <code>true</code> if the building is on fire.
     */
    @Override
    public boolean isBurning(Coords coords) {
        return building.isBurning(boardToRelative(coords));
    }

    /**
     * Set the flag that indicates that this building is on fire.
     *
     * @param onFire - a <code>boolean</code> value that indicates whether this building is on fire.
     * @param coords BOARD coordinates
     */
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

    /**
     * Remove one building hex from the building
     *
     * @param coords - the BOARD <code>Coords</code> of the hex to be removed
     */
    @Override
    public void removeHex(Coords coords) {
        CubeCoords relative = boardToRelative(coords);
        building.removeHex(relative);
        // Remove from mappings
        boardToRelativeMap.remove(coords);
        relativeToBoardMap.remove(relative);
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

    @Override
    public int getBoardId() {
        return building.getBoardId();
    }

    @Override
    public void setBoardId(int boardId) {
        building.setBoardId(boardId);
    }
}
