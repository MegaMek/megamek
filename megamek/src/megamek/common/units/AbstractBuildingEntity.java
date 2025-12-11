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
import java.util.Vector;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.cost.CostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.rolls.PilotingRollData;
import megamek.logging.MMLogger;

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
    private final Map<CubeCoords, Coords> relativeLayout = new HashMap<>();  // Relative CubeCoords -> actual board coords

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
     * Updates the relativeLayout map to reflect the current building configuration.
     * Maps each relative CubeCoord in the building to its actual board position.
     */
    private void updateRelativeLayout() {
        relativeLayout.clear();

        if (getPosition() == null) {
            return;
        }

        secondaryPositions.put(0, getPosition());
        relativeLayout.put(CubeCoords.ZERO, getPosition());

        // Map each relative CubeCoord to its actual board coordinate
        int position = 1;
        for (CubeCoords relCoord : building.getCoordsList()) {
            // We add the origin manually
            if (!relCoord.equals(CubeCoords.ZERO)) {
                // TODO: When rotation is implemented, rotate by facing before adding to entity position
                // For now, with no rotation, convert CubeCoord to offset and add to entity position
                CubeCoords positionCubeCoords = getPosition().toCube();
                Coords boardCoord = positionCubeCoords.add(relCoord).toOffset();

                relativeLayout.put(relCoord, boardCoord);
                secondaryPositions.put(position, boardCoord);
                position++;
            }
        }
    }

    /**
     * Returns the number of locations in the entity
     */
    @Override
    public int locations() {
        if (getInternalBuilding() == null || getInternalBuilding().getCoordsList() == null) {
            return 1;
        }
        return getInternalBuilding().getOriginalHexCount() * getInternalBuilding().getBuildingHeight();
    }

    public void refreshAdditionalLocations() {
        armorType = new int[locations()];
        armorTechLevel = new int[locations()];
        hardenedArmorDamaged = new boolean[locations()];
        locationBlownOff = new boolean[locations()];
        locationBlownOffThisPhase = new boolean[locations()];
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
        ArrayList<String> locationNames = new ArrayList<String>();
        if (getInternalBuilding() == null || getInternalBuilding().getCoordsList() == null) {
            return new String[] { LOCATION_NAMES_PREFIX + ' ' + LOC_BASE };
        }
        for (CubeCoords coord : getInternalBuilding().getCoordsList()) {
            // Starts at Level 0 for ground floor
            // Can't use boardNum when unit isn't deployed or negative hexes break
            String coordString = getPosition() != null ? coord.toOffset().getBoardNum() :
                  coord.q() + "," + coord.r() + "," + coord.s();
            for (int level = 0; level < getInternalBuilding().getBuildingHeight(); level++) {
                locationNames.add(LOCATION_NAMES_PREFIX + ' ' + level + ' ' + coordString);
            }
        }
        return locationNames.toArray(new String[0]);
    }

    @Override
    public String[] getLocationAbbreviations() {
        ArrayList<String> locationAbbrvNames = new ArrayList<String>();
        if (getInternalBuilding() == null || getInternalBuilding().getCoordsList() == null) {
            return new String[] { LOCATION_ABBREVIATIONS_PREFIX + ' ' + LOC_BASE };
        }
        for (CubeCoords coord : getInternalBuilding().getCoordsList()) {
            String coordString = getPosition() != null ? coord.toOffset().getBoardNum() :
                  coord.q() + "," + coord.r() + "," + coord.s();
            for (int level = 0; level < getInternalBuilding().getBuildingHeight(); level++) {
                locationAbbrvNames.add(LOCATION_ABBREVIATIONS_PREFIX + ' ' + level + ' ' + coordString);
            }
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
            .map(this::relativeToBoard)
            .filter(c -> c != null)
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
        relativeLayout.remove(relative);
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
}
