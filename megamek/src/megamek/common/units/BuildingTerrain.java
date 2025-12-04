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

import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;

public class BuildingTerrain implements IBuilding {
    private Building building;

    public BuildingTerrain(Coords coords, Board board, int structureType, BasementType basementType) {
        building = new Building(coords, board, structureType, basementType);
    }

    @Override
    public IBuilding getBuilding() {
        return building;
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
     * @param coords - the <code>Coords</code> being examined.
     *
     * @return <code>true</code> if the building has CF at the coordinates.
     *       <code>false</code> otherwise.
     */
    @Override
    public boolean hasCFIn(Coords coords) {
        return building.hasCFIn(coords);
    }

    /**
     * Get the coordinates that the building occupies.
     *
     * @return an <code>Enumeration</code> of the <code>Coord</code> objects.
     */
    @Override
    public Enumeration<Coords> getCoords() {
        return building.getCoords();
    }

    /**
     * Returns a list of this Building's coords. The list is unmodifiable.
     */
    @Override
    public List<Coords> getCoordsList() {
        return building.getCoordsList();
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
     * @param coords
     *
     * @return the <code>int</code> code of the building basement type.
     */
    @Override
    public boolean getBasementCollapsed(Coords coords) {
        return building.getBasementCollapsed(coords);
    }

    @Override
    public void collapseBasement(Coords coords, Board board, Vector<Report> vPhaseReport) {
        building.collapseBasement(coords, board, vPhaseReport);
    }

    /**
     * Roll what kind of basement this building has
     *
     * @param coords       the <code>Coords</code> of the building to roll for
     * @param board
     * @param vPhaseReport the {@link Report} <code>Vector</code> containing the phase report
     *
     * @return a <code>boolean</code> indicating weather the hex and building was changed or not
     */
    @Override
    public boolean rollBasement(Coords coords, Board board, Vector<Report> vPhaseReport) {
        return building.rollBasement(coords, board, vPhaseReport);
    }

    /**
     * Get the current construction factor of the building hex at the passed coords. Any damage immediately updates this
     * value.
     *
     * @param coords the <code>Coords</code> of the hex in question
     *
     * @return the <code>int</code> value of the building hex's current construction factor. This value will be greater
     *       than or equal to zero.
     */
    @Override
    public int getCurrentCF(Coords coords) {
        return building.getCurrentCF(coords);
    }

    /**
     * Get the construction factor of the building hex at the passed coords at the start of the current phase. Damage
     * that is received during the phase is applied at the end of the phase.
     *
     * @param coords the <code>Coords</code> of the hex in question
     *
     * @return the <code>int</code> value of the building's construction factor at the start of this phase. This value
     *       will be greater than or equal to zero.
     */
    @Override
    public int getPhaseCF(Coords coords) {
        return building.getPhaseCF(coords);
    }

    @Override
    public int getArmor(Coords coords) {
        return building.getArmor(coords);
    }

    /**
     * Set the current construction factor of the building hex. Call this method immediately when the building sustains
     * any damage.
     *
     * @param cf     the <code>int</code> value of the building hex's current construction factor. This value must be
     *               greater than or equal to zero.
     * @param coords the <code>Coords</code> of the hex in question
     *
     * @throws IllegalArgumentException if the passed value is less than zero
     */
    @Override
    public void setCurrentCF(int cf, Coords coords) {
        building.setCurrentCF(cf, coords);
    }

    /**
     * Set the construction factor of the building hex for the start of the next phase. Call this method at the end of
     * the phase to apply damage sustained by the building during the phase.
     *
     * @param cf     the <code>int</code> value of the building hex's current construction factor. This value must be
     *               greater than or equal to zero.
     * @param coords the <code>Coords</code> of the hex in question
     *
     * @throws IllegalArgumentException if the passed value is less than zero
     */
    @Override
    public void setPhaseCF(int cf, Coords coords) {
        building.setPhaseCF(cf, coords);
    }

    @Override
    public void setArmor(int a, Coords coords) {
        building.setArmor(a, coords);
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
     * @param coords
     *
     * @return <code>true</code> if the building is on fire.
     */
    @Override
    public boolean isBurning(Coords coords) {
        return building.isBurning(coords);
    }

    /**
     * Set the flag that indicates that this building is on fire.
     *
     * @param onFire - a <code>boolean</code> value that indicates whether this building is on fire.
     * @param coords
     */
    @Override
    public void setBurning(boolean onFire, Coords coords) {
        building.setBurning(onFire, coords);
    }

    @Override
    public void addDemolitionCharge(int playerId, int damage, Coords pos) {
        building.addDemolitionCharge(playerId, damage, pos);
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
     * @param coords - the <code>Coords</code> of the hex to be removed
     */
    @Override
    public void removeHex(Coords coords) {
        building.removeHex(coords);
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
        return building.getBasement(coords);
    }

    @Override
    public void setBasement(Coords coords, BasementType basement) {
        building.setBasement(coords, basement);
    }

    @Override
    public void setBasementCollapsed(Coords coords, boolean collapsed) {
        building.setBasementCollapsed(coords, collapsed);
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
