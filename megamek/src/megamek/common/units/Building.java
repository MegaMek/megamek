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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.rolls.Roll;
import megamek.logging.MMLogger;

/**
 * This class represents a single, possibly multi-hex building on the board.
 * <p>
 * FIXME : This needs a complete rewrite to properly handle the latest building
 * rules
 * <p>
 * Rewrite Notes:
 * TODO : 1) Migrate Magic Numbers to Enums
 * TODO : 2) Offboard Gun Emplacements: Revisit with a required rules query
 * (CustomMekDialog - 22-Feb-2022)
 *
 * @author Suvarov454@sourceforge.net (James A. Damour)
 */
public class Building implements Serializable {

    @Serial
    private static final long serialVersionUID = -8236017592012683793L;

    private static final MMLogger logger = MMLogger.create(Building.class);

    /**
     * The Building Type of the building; equal to the terrain elevation of the BUILDING terrain of a hex.
     */
    private final BuildingType type;

    /**
     * The Building Class of the building; equal to the terrain elevation of the BUILDING CLASS terrain of a hex.
     */
    private final int bldgClass;

    /**
     * The height of the building (BLDG_ELEV). Individual hexes start with this height and can be set to 0 when destroyed.
     */
    private int buildingHeight;

    /**
     * The ID of this building.
     */
    private final int id;

    /**
     * The coordinates of every hex of this building.
     */
    private final Vector<CubeCoords> coordinates = new Vector<>();
    private final Vector<CubeCoords> originalCoordinates = new Vector<>();
    private int boardId;

    /**
     * The Basement type of the building.
     */
    private final Map<CubeCoords, BasementType> basement = new HashMap<>();

    private int collapsedHexes = 0;

    private int originalHexes = 0;

    /**
     * The current construction factor of the building hexes. Any damage immediately updates this value.
     */
    private final Map<CubeCoords, Integer> currentCF = new HashMap<>();

    /**
     * The construction factor of the building hexes at the start of this attack phase. Damage that is received during
     * the phase is applied at the end of the phase.
     */
    private final Map<CubeCoords, Integer> phaseCF = new HashMap<>();

    /**
     * The current armor of the building hexes.
     */
    private final Map<CubeCoords, Integer> armor = new HashMap<>();

    /**
     * The height of the building hexes (BLDG_ELEV).
     */
    private final Map<CubeCoords, Integer> height = new HashMap<>();

    /**
     * The current state of the basement.
     */
    private final Map<CubeCoords, Boolean> basementCollapsed = new HashMap<>();

    /**
     * The name of the building.
     */
    private final String name;

    /**
     * Flag that indicates whether this building is burning
     */
    private final Map<CubeCoords, Boolean> burning = new HashMap<>();

    private List<DemolitionCharge> demolitionCharges = new ArrayList<>();

    /**
     * Determine if the building occupies given coordinates. Multi-hex buildings will occupy multiple coordinates. Only
     * one building per hex.
     *
     * @param coords - the <code>CubeCoords</code> being examined.
     *
     * @return <code>true</code> if the building occupies the coordinates.
     *       <code>false</code> otherwise.
     */
    public boolean isIn(CubeCoords coords) {
        return coordinates.contains(coords);
    }

    /**
     * Add a hex to this building at the given RELATIVE coordinates.
     * All coordinates are relative to the building's origin (0,0,0).
     *
     * Building stores everything in its own local coordinate space using CubeCoords.
     * BuildingTerrain and AbstractBuildingEntity handle translation between board Coords and relative CubeCoords.
     *
     * @param relativeCoords the relative <code>CubeCoords</code> of the hex within the building
     * @param cf the construction factor for this hex
     * @param armorValue the armor value for this hex
     * @param basementType the basement type for this hex
     * @param collapsed whether the basement is collapsed
     */
    public void addHex(CubeCoords relativeCoords, int cf, int armorValue, BasementType basementType, boolean collapsed) {
        if (isIn(relativeCoords)) {
            return; // Already added
        }

        coordinates.addElement(relativeCoords);
        originalCoordinates.addElement(relativeCoords);
        originalHexes++;
        currentCF.put(relativeCoords, cf);
        phaseCF.put(relativeCoords, cf);
        armor.put(relativeCoords, armorValue);
        basement.put(relativeCoords, basementType);
        basementCollapsed.put(relativeCoords, collapsed);
        burning.put(relativeCoords, false);
        height.put(relativeCoords, buildingHeight);
    }

    public Building(BuildingType type, int bldgClass, int id, int structureType) {
        this.type = type;
        this.bldgClass = bldgClass;
        this.id = id;


        // Set the building's name.
        StringBuilder sb = new StringBuilder();
        if (structureType == Terrains.FUEL_TANK) {
            sb.append("Fuel Tank #");
        } else if (getBuildingType() == BuildingType.WALL) {
            sb.append("Wall #");
        } else if (structureType == Terrains.BUILDING) {
            sb.append("Building #");
        } else if (structureType == Terrains.BRIDGE) {
            sb.append("Bridge #");
        } else {
            sb.append("Structure #");
        }
        sb.append(id);
        name = sb.toString();
    }


    /**
     * Get the ID of this building. The same ID applies to all hexes.
     *
     * @return the <code>int</code> ID of the building.
     */
    public int getId() {
        return id;
    }

    /**
     * Determines if the coord exist in the currentCF has.
     *
     * @param coords - the <code>CubeCoords</code> being examined.
     *
     * @return <code>true</code> if the building has CF at the coordinates.
     *       <code>false</code> otherwise.
     */
    public boolean hasCFIn(CubeCoords coords) {
        return currentCF.containsKey(coords);
    }

    /**
     * Get the coordinates that the building occupies.
     *
     * @return an <code>Enumeration</code> of the <code>CubeCoord</code> objects.
     */
    public Enumeration<CubeCoords> getCoords() {
        return coordinates.elements();
    }

    /** Returns a list of this Building's coords. The list is unmodifiable. */
    public List<CubeCoords> getCoordsList() {
        return Collections.unmodifiableList(coordinates);
    }

    /** Returns a list of this Building's original coords (before any hexes were removed). The list is unmodifiable. */
    public List<CubeCoords> getOriginalCoordsList() {
        return Collections.unmodifiableList(originalCoordinates);
    }

    /**
     * Get the construction type of the building. This value will be one of the values defined in
     * megamek.common.enums.BuildingType
     *
     * @return the <code>int</code> code of the building's construction type.
     */
    
    public BuildingType getBuildingType() {
        return type;
    }

    /**
     * Get the building class, per TacOps rules.
     *
     * @return the <code>int</code> code of the building's classification.
     */
    
    public int getBldgClass() {
        return bldgClass;
    }

    /**
     * Get the building basement, per TacOps rules.
     *
     * @return the <code>int</code> code of the building basement type.
     */
    public boolean getBasementCollapsed(CubeCoords coords) {
        return basementCollapsed.get(coords);
    }

    public void collapseBasement(CubeCoords coords, Board board, Vector<Report> vPhaseReport) {
        // Building works in relative CubeCoords
        // BuildingTerrain/BuildingEntity handle board coord translation and Board updates
        if (basement.get(coords).isNone() || basement.get(coords).isOneDeepNormalInfantryOnly()) {
            logger.error("Hex has no basement to collapse");
            return;
        } else if (basementCollapsed.get(coords)) {
            logger.error("Hex has basement that already collapsed");
            return;
        }
        Report r = new Report(2112, Report.PUBLIC);
        r.add(getName());
        r.add(coords.toOffset().getBoardNum());
        vPhaseReport.add(r);
        logger.error("basement {}is collapsing at relative coords: {}", basement, coords);
        basementCollapsed.put(coords, true);
        // Note: BuildingTerrain/Entity will handle board.getHex(boardCoords).addTerrain()
    }

    /**
     * Roll what kind of basement this building has at the given RELATIVE coordinates.
     * Building works in relative CubeCoords - BuildingTerrain/BuildingEntity handle board coord translation.
     *
     * @param coords       the RELATIVE <code>CubeCoords</code> of the building hex to roll for
     * @param vPhaseReport the {@link Report} <code>Vector</code> containing the phase report
     *
     * @return a <code>boolean</code> indicating whether the hex and building was changed or not
     */
    public boolean rollBasement(CubeCoords coords, Board board, Vector<Report> vPhaseReport) {
        if (basement.get(coords).isUnknown()) {
            Report r = new Report(2111, Report.PUBLIC);
            r.add(getName());
            r.add(coords.toOffset().getBoardNum());
            Roll diceRoll = Compute.rollD6(2);
            r.add(diceRoll);

            BasementType rolledType;
            if (diceRoll.getIntValue() == 2) {
                rolledType = BasementType.TWO_DEEP_FEET;
            } else if (diceRoll.getIntValue() == 3) {
                rolledType = BasementType.ONE_DEEP_FEET;
            } else if (diceRoll.getIntValue() == 4 || diceRoll.getIntValue() == 10) {
                rolledType = BasementType.ONE_DEEP_NORMAL;
            } else if (diceRoll.getIntValue() == 11) {
                rolledType = BasementType.ONE_DEEP_HEAD;
            } else if (diceRoll.getIntValue() == 12) {
                rolledType = BasementType.TWO_DEEP_HEAD;
            } else {
                rolledType = BasementType.NONE;
            }

            basement.put(coords, rolledType);
            r.add(rolledType.toString());
            vPhaseReport.add(r);
            // Note: BuildingTerrain/Entity will handle board.getHex(boardCoords).addTerrain()
            return true;
        }

        return false;
    }

    /**
     * Get the current construction factor of the building hex at the passed coords. Any damage immediately updates this
     * value.
     *
     * @param coords the <code>CubeCoords</code> of the hex in question
     *
     * @return the <code>int</code> value of the building hex's current construction factor. This value will be greater
     *       than or equal to zero.
     */
    public int getCurrentCF(CubeCoords coords) {
        return currentCF.getOrDefault(coords, 0);
    }

    /**
     * Get the construction factor of the building hex at the passed coords at the start of the current phase. Damage
     * that is received during the phase is applied at the end of the phase.
     *
     * @param coords the <code>CubeCoords</code> of the hex in question
     *
     * @return the <code>int</code> value of the building's construction factor at the start of this phase. This value
     *       will be greater than or equal to zero.
     */
    public int getPhaseCF(CubeCoords coords) {
        return phaseCF.getOrDefault(coords, 0);
    }

    public int getArmor(CubeCoords coords) {
        return armor.getOrDefault(coords, 0);
    }

    /**
     * Set the current construction factor of the building hex. Call this method immediately when the building sustains
     * any damage.
     *
     * @param coords the <code>CubeCoords</code> of the hex in question
     * @param cf     the <code>int</code> value of the building hex's current construction factor. This value must be
     *               greater than or equal to zero.
     *
     * @throws IllegalArgumentException if the passed value is less than zero
     */
    public void setCurrentCF(int cf, CubeCoords coords) {
        if (cf < 0) {
            throw new IllegalArgumentException("Invalid value for Construction Factor: " + cf);
        }

        currentCF.put(coords, cf);
    }

    /**
     * Set the construction factor of the building hex for the start of the next phase. Call this method at the end of
     * the phase to apply damage sustained by the building during the phase.
     *
     * @param coords the <code>CubeCoords</code> of the hex in question
     * @param cf     the <code>int</code> value of the building hex's current construction factor. This value must be
     *               greater than or equal to zero.
     *
     * @throws IllegalArgumentException if the passed value is less than zero
     */
    public void setPhaseCF(int cf, CubeCoords coords) {
        if (cf < 0) {
            throw new IllegalArgumentException(
                  "Invalid value for Construction Factor: " + cf);
        }

        phaseCF.put(coords, cf);
    }

    public void setArmor(int a, CubeCoords coords) {
        if (a < 0) {
            throw new IllegalArgumentException("Invalid value for armor: " + a);
        }

        armor.put(coords, a);
    }

    /**
     * Get the height (BLDG_ELEV) of the building hex at the passed coords.
     *
     * @param coords the <code>CubeCoords</code> of the hex in question
     *
     * @return the <code>int</code> value of the building hex's height. This value will be 0 if the hex is destroyed.
     */
    public int getHeight(CubeCoords coords) {
        return height.getOrDefault(coords, 0);
    }

    /**
     * Set the height of a specific building hex. Call this method when a hex is destroyed (set to 0).
     *
     * @param coords the <code>CubeCoords</code> of the hex in question
     * @param h      the <code>int</code> value of the building hex's height. This value must be greater than or equal to
     *               zero.
     *
     * @throws IllegalArgumentException if the passed value is less than zero
     */
    public void setHeight(int h, CubeCoords coords) {
        if (h < 0) {
            throw new IllegalArgumentException("Invalid value for height: " + h);
        }

        height.put(coords, h);
    }

    /**
     * Get the default height of the building. All hexes start with this height.
     *
     * @return the <code>int</code> value of the building's height (BLDG_ELEV).
     */
    public int getBuildingHeight() {
        return buildingHeight;
    }

    /**
     * Set the default height of the building. This should be called when creating a BuildingTerrain from board data.
     *
     * @param buildingHeight the <code>int</code> value of the building's height (BLDG_ELEV).
     */
    public void setBuildingHeight(int buildingHeight) {
        this.buildingHeight = buildingHeight;
    }

    /**
     * Get the name of this building.
     *
     * @return the <code>String</code> name of this building.
     */
    
    public String getName() {
        return name;
    }

    /**
     * Two Buildings are equal if and only if their IDs are equal.
     *
     * @param other The other Object to compare
     *
     * @return True if this and the given other are considered equal
     */
    @Override
    public boolean equals(Object other) {
        return (this == other) || ((other instanceof Building otherBuilding) && (getId() == otherBuilding.getId()));
    }

    @Override
    public int hashCode() {
        return id;
    }

    /**
     * Returns a string representation of the given building class, e.g. "Hangar".
     */
    public static String className(int bldgClass) {
        return switch (bldgClass) {
            case IBuilding.HANGAR -> "Hangar";
            case IBuilding.FORTRESS -> "Fortress";
            case IBuilding.GUN_EMPLACEMENT -> "Gun Emplacement";
            default -> "Building";
        };
    }

    @Override
    public String toString() {
        return getBuildingType().toString() + " " + className(getBldgClass()) + " " + name;
    }

    /**
     * Determine if this building is on fire.
     *
     * @return <code>true</code> if the building is on fire.
     */
    public boolean isBurning(CubeCoords coords) {
        return burning.get(coords);
    }

    /**
     * Set the flag that indicates that this building is on fire.
     *
     * @param onFire - a <code>boolean</code> value that indicates whether this building is on fire.
     */
    public void setBurning(boolean onFire, CubeCoords coords) {
        burning.put(coords, onFire);
    }

    public void addDemolitionCharge(int playerId, int damage, CubeCoords pos) {
        DemolitionCharge charge = new DemolitionCharge(playerId, damage, pos.toOffset());
        demolitionCharges.add(charge);
    }

    
    public void removeDemolitionCharge(DemolitionCharge charge) {
        demolitionCharges.remove(charge);
    }

    
    public List<DemolitionCharge> getDemolitionCharges() {
        return demolitionCharges;
    }

    
    public void setDemolitionCharges(List<DemolitionCharge> charges) {
        demolitionCharges = charges;
    }

    /**
     * Remove one building hex from the building
     *
     * @param coords - the <code>CubeCoords</code> of the hex to be removed
     */
    public void removeHex(CubeCoords coords) {
        coordinates.remove(coords);
        currentCF.remove(coords);
        phaseCF.remove(coords);
        collapsedHexes++;
    }

    public int getOriginalHexCount() {
        return originalHexes;
    }

    public int getCollapsedHexCount() {
        return collapsedHexes;
    }

    public BasementType getBasement(CubeCoords coords) {
        return basement.get(coords);
    }

    public void setBasement(CubeCoords coords, BasementType basement) {
        this.basement.put(coords, basement);
    }

    public void setBasementCollapsed(CubeCoords coords, boolean collapsed) {
        basementCollapsed.put(coords, collapsed);
    }

    
    public int getBoardId() {
        return boardId;
    }

    public void
    setBoardId(int boardId) {
        this.boardId = boardId;
    }
}
