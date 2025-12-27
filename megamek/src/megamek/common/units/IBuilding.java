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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;

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
public interface IBuilding extends Serializable {

    int UNKNOWN = -1;

    // The Building Classes
    int STANDARD = 0;
    int HANGAR = 1;
    int FORTRESS = 2;
    int GUN_EMPLACEMENT = 3;
    // TODO: leaving out Castles Brian until issues with damage scaling are resolved
    // public static final int CASTLE_BRIAN = 3;

    static int currentId(Board board, Coords coords) {
        if (board != null && coords != null) {
            return board.getBoardId() * 1_000_000 + coords.getX() * 1000 + coords.getY();
        }
        return UNKNOWN;
    }

    /**
     * Get the ID of this building. The same ID applies to all hexes.
     *
     * @return the <code>int</code> ID of the building.
     */
    int getId();

    /**
     * Determine if the building occupies given coordinates. Multi-hex buildings will occupy multiple coordinates. Only
     * one building per hex.
     *
     * @param coords - the <code>Coords</code> being examined.
     *
     * @return <code>true</code> if the building occupies the coordinates.
     *       <code>false</code> otherwise.
     */
    default boolean isIn(Coords coords) {
        if (coords != null) {
            return getCoordsList().contains(coords);
        }
        return false;
    }

    /**
     * Determines if the coord exist in the currentCF has.
     *
     * @param coords - the <code>Coords</code> being examined.
     *
     * @return <code>true</code> if the building has CF at the coordinates.
     *       <code>false</code> otherwise.
     */
    boolean hasCFIn(Coords coords);

    /**
     * Get the coordinates that the building occupies.
     *
     * @return an <code>Enumeration</code> of the <code>Coord</code> objects.
     */
    Enumeration<Coords> getCoords();


    /** Returns a list of this Building's coords. The list is unmodifiable. */
    List<Coords> getCoordsList();

    /**
     * Get the construction type of the building. This value will be one of the values defined in
     * megamek.common.enums.BuildingType
     *
     * @return the <code>int</code> code of the building's construction type.
     */
    BuildingType getBuildingType();

    /**
     * Get the building class, per TacOps rules.
     *
     * @return the <code>int</code> code of the building's classification.
     */
    int getBldgClass();

    /**
     * Get the building basement, per TacOps rules.
     *
     * @return the <code>int</code> code of the building basement type.
     */
    boolean getBasementCollapsed(Coords coords);

    void collapseBasement(Coords coords, Board board, Vector<Report> vPhaseReport);

    /**
     * Roll what kind of basement this building has
     *
     * @param coords       the <code>Coords</code> of the building to roll for
     * @param vPhaseReport the {@link Report} <code>Vector</code> containing the phase report
     *
     * @return a <code>boolean</code> indicating weather the hex and building was changed or not
     */
    boolean rollBasement(Coords coords, Board board, Vector<Report> vPhaseReport);

    /**
     * Get the current construction factor of the building hex at the passed coords. Any damage immediately updates this
     * value.
     *
     * @param coords the <code>Coords</code> of the hex in question
     *
     * @return the <code>int</code> value of the building hex's current construction factor. This value will be greater
     *       than or equal to zero.
     */
    int getCurrentCF(Coords coords);

    /**
     * Get the construction factor of the building hex at the passed coords at the start of the current phase. Damage
     * that is received during the phase is applied at the end of the phase.
     *
     * @param coords the <code>Coords</code> of the hex in question
     *
     * @return the <code>int</code> value of the building's construction factor at the start of this phase. This value
     *       will be greater than or equal to zero.
     */
    int getPhaseCF(Coords coords);

    int getArmor(Coords coords);

    /**
     * Get the height (BLDG_ELEV) of the building hex at the passed coords.
     *
     * @param coords the <code>Coords</code> of the hex in question
     *
     * @return the <code>int</code> value of the building hex's height. This value will be 0 if the hex is destroyed.
     */
    int getHeight(Coords coords);

    /**
     * Set the current construction factor of the building hex. Call this method immediately when the building sustains
     * any damage.
     *
     * @param coords the <code>Coords</code> of the hex in question
     * @param cf     the <code>int</code> value of the building hex's current construction factor. This value must be
     *               greater than or equal to zero.
     *
     * @throws IllegalArgumentException if the passed value is less than zero
     */
    void setCurrentCF(int cf, Coords coords);

    /**
     * Set the construction factor of the building hex for the start of the next phase. Call this method at the end of
     * the phase to apply damage sustained by the building during the phase.
     *
     * @param coords the <code>Coords</code> of the hex in question
     * @param cf     the <code>int</code> value of the building hex's current construction factor. This value must be
     *               greater than or equal to zero.
     *
     * @throws IllegalArgumentException if the passed value is less than zero
     */
    void setPhaseCF(int cf, Coords coords);

    void setArmor(int a, Coords coords);

    /**
     * Set the height of a specific building hex. Call this method when a hex is destroyed (set to 0).
     *
     * @param h      the <code>int</code> value of the building hex's height. This value must be greater than or equal to
     *               zero.
     * @param coords the <code>Coords</code> of the hex in question
     *
     * @throws IllegalArgumentException if the passed value is less than zero
     */
    void setHeight(int h, Coords coords);

    /**
     * Get the name of this building.
     *
     * @return the <code>String</code> name of this building.
     */
    String getName();

    /**
     * Get the default construction factor for the given type of building.
     */
    static int getDefaultCF(BuildingType type) {
        return type.getDefaultCF();
    }

    /**
     * Get the default construction factor for the given type of building. Retained for backwards compatibility
     *
     * @param type - the <code>int</code> construction type of the building.
     *
     * @return the <code>int</code> default construction factor for that type of building. If a bad type value is
     *       passed, the constant
     *       <code>Building.UNKNOWN</code> will be returned instead.
     */
    static int getDefaultCF(int type) {
        return getDefaultCF(BuildingType.getType(type));
    }

    /**
     * Returns a string representation of the given building class, e.g. "Hangar".
     */
    static String className(int bldgClass) {
        return switch (bldgClass) {
            case IBuilding.HANGAR -> "Hangar";
            case IBuilding.FORTRESS -> "Fortress";
            case IBuilding.GUN_EMPLACEMENT -> "Gun Emplacement";
            default -> "Building";
        };
    }

    /**
     * Determine if this building is on fire.
     *
     * @return <code>true</code> if the building is on fire.
     */
    boolean isBurning(Coords coords);

    /**
     * Set the flag that indicates that this building is on fire.
     *
     * @param onFire - a <code>boolean</code> value that indicates whether this building is on fire.
     */
    public void setBurning(boolean onFire, Coords coords);

    public void addDemolitionCharge(int playerId, int damage, Coords pos);

    public void removeDemolitionCharge(DemolitionCharge charge);

    public List<DemolitionCharge> getDemolitionCharges();

    public void setDemolitionCharges(List<DemolitionCharge> charges);

    /**
     * Remove one building hex from the building
     *
     * @param coords - the <code>Coords</code> of the hex to be removed
     */
    public void removeHex(Coords coords);

    public int getOriginalHexCount();

    public int getCollapsedHexCount();

    /**
     * @return the damage scale multiplier for units passing through this building
     */
    default double getDamageFromScale() {
        return switch (getBldgClass()) {
            case IBuilding.HANGAR -> 0.5;
            case IBuilding.FORTRESS, IBuilding.GUN_EMPLACEMENT -> 2.0;
            default -> 1.0;
        };
    }

    /**
     * @return the damage scale multiplier for damage applied to this building (and occupants)
     */
    default double getDamageToScale() {
        return switch (getBldgClass()) {
            case IBuilding.FORTRESS, IBuilding.GUN_EMPLACEMENT -> 0.5;
            default -> 1.0;
        };
    }

    /**
     * @return the amount of damage the building absorbs
     */
    default int getAbsorption(Coords pos) {
        return (int) Math.ceil(getPhaseCF(pos) / 10.0);
    }

    /**
     * Returns the percentage of damage done to the building for attacks against infantry in the building from other
     * units within the building. TW pg175.
     */
    default double getInfDmgFromInside() {
        return switch (getBuildingType()) {
            case LIGHT, MEDIUM -> 0.0;
            case HEAVY -> 0.5;
            case HARDENED -> 0.75;
            default -> 0;
        };
    }

    /**
     * Per page 172 of Total Warfare, this is the fraction of a weapon's damage that passes through to infantry inside
     * the building.
     *
     * @return Damage fraction.
     */
    default float getDamageReductionFromOutside() {
        return switch (getBuildingType()) {
            case LIGHT -> 0.75f;
            case MEDIUM -> 0.5f;
            case HEAVY -> 0.25f;
            default -> 0f;
        };
    }

    public BasementType getBasement(Coords coords);

    public void setBasement(Coords coords, BasementType basement);

    public void setBasementCollapsed(Coords coords, boolean collapsed);

    public int getBoardId();

    public void setBoardId(int boardId);

    // ========== Coordinate Translation Support ==========
    // Subclasses can implement these to enable board<->relative coordinate translation

    /**
     * Get the board coordinates of this building's origin (where relative 0,0 maps to).
     * For BuildingTerrain: the static placement coords
     * For AbstractBuildingEntity: the entity's current position
     *
     * @return the board coordinates of the origin, or null if not placed
     */
    default Coords getBoardOrigin() {
        return null;  // Building uses relative coords directly, so no origin
    }

    /**
     * Get the facing/rotation of this building in board space.
     * 0 = north, 1-5 = clockwise rotation
     *
     * @return the facing (0-5)
     */
    default int getBoardFacing() {
        return 0;  // No rotation by default
    }

    /**
     * Get the internal Building instance that stores data in relative coordinates.
     * Building returns itself; BuildingTerrain/BuildingEntity override to return wrapped instance.
     *
     * @return the internal Building instance
     */
    default Building getInternalBuilding() {
        return (Building) this;  // Building returns itself
    }

    /**
     * Convert board coordinates to relative CubeCoordinates.
     * Default: convert board Coords to CubeCoords.
     * BuildingTerrain/BuildingEntity override to translate using their coord maps.
     *
     * @param boardCoords the board coordinates
     * @return the relative CubeCoordinates
     */
    default CubeCoords boardToRelative(Coords boardCoords) {
        return boardCoords.toCube();  // Convert to CubeCoords by default
    }

    /**
     * Convert relative CubeCoordinates to board coordinates.
     * Default: convert CubeCoords to offset Coords.
     * BuildingTerrain/BuildingEntity override to translate using their coord maps.
     *
     * @param relativeCoords the relative CubeCoordinates
     * @return the board coordinates
     */
    default Coords relativeToBoard(CubeCoords relativeCoords) {
        return relativeCoords.toOffset();  // Convert to offset Coords by default
    }
}
