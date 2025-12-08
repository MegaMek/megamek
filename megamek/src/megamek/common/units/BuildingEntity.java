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
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.MPCalculationSetting;
import megamek.common.Report;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.cost.CostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.rolls.PilotingRollData;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * BuildingEntity represents a mobile building (e.g., a moving fortress).
 *
 * It contains a Building (which stores data in relative coordinates) and handles
 * translation between board coordinates and the Building's relative coordinate space.
 * Unlike BuildingTerrain, the translation is dynamic based on Entity's current position/facing.
 */
public class BuildingEntity extends Entity implements IBuilding {

    private static final MMLogger logger = MMLogger.create(BuildingEntity.class);

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

    private static final String LOCATION_ABBREVIATIONS_PREFIX = "FLR";
    private static final String LOCATION_NAMES_PREFIX = "Floor";

    private static final int[] CRITICAL_SLOTS = new int[] { 100 };

    public BuildingEntity(BuildingType type, int bldgClass) {
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
        for (CubeCoords relCoord : getInternalBuilding().getOriginalCoordsList()) {
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

    // FIXME: IDK if this is right, just needed something to pass tests
    private static final TechAdvancement TA_BUILDING_ENTITY = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
          .setTechRating(TechRating.B)
          .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    /**
     * @see UnitType
     */
    @Override
    public int getUnitType() {
        return 0;
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
            String coordString = getPosition() != null ? relativeToBoard(cubeCoords).getBoardNum() : cubeCoords.q() + "," + cubeCoords.r() + "," + cubeCoords.s();
            // Result is 0 indexed, let's make it 1 indexed so it makes more sense to players
            int level = (location % getInternalBuilding().getBuildingHeight()) + 1;
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
        WeaponMounted weapon = getWeapon(weaponNumber);
        if (weapon.isTurret()) {
            return 0;
        }
        switch(weapon.getFacing()) {
            case 0:
                return 1;
            case 1:
                return 50;
            case 2:
                return 51;
            case 3:
                return 52;
            case 4:
                return 53;
            case 5:
                return 54;
            default: return 0;
        }
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
     * @return int
     */
    @Override
    public int getWeaponFiringHeight(WeaponMounted weapon) {
        if (weapon == null) {
            return super.getWeaponFiringHeight(weapon);
        }
        int location = weapon.getLocation();
        return location % getInternalBuilding().getBuildingHeight();
    }

    @Override
    public int[] getNoOfSlots() {
        return CRITICAL_SLOTS;
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

    /**
     * The maximum elevation change the entity can cross
     */
    @Override
    public int getMaxElevationChange() {
        return 0;
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
    public boolean hasEngine() {
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
        for (CubeCoords relCoord : getInternalBuilding().getCoordsList()) {
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
        return getInternalBuilding().getCoordsList().stream()
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
     * Replaced most instances of `instanceof GunEmplacement` to support {@link BuildingEntity}
     * @return true if this unit is a {@link BuildingEntity} or {@link GunEmplacement}, false otherwise
     */
    @Override
    public boolean isBuildingEntityOrGunEmplacement() {
        return true;
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

    public void applyCollapsedHexLocationDamage(Coords coords) {
        for (int floor = 0; floor < getInternalBuilding().getBuildingHeight(); floor++) {
            applyCollapseFloorLocationDamage(coords, floor);
        }
    }

    /**
     *
     * @param coords
     * @param floor
     */
    public void applyCollapseFloorLocationDamage(Coords coords, int floor) {
        for (int location : locationToRelativeCoordsMap.keySet()) {
            if (coords.equals(relativeToBoard(locationToRelativeCoordsMap.get(location)))) {
                if (location % getInternalBuilding().getBuildingHeight() == floor) {
                    setInternal(0, location);
                    destroyLocation(location, true);
                }
            }
        }
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
}

