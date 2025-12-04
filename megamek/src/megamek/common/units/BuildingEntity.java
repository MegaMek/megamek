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

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.MPCalculationSetting;
import megamek.common.Report;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.cost.CostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Mounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.rolls.PilotingRollData;
import megamek.logging.MMLogger;

public class BuildingEntity extends Entity implements IBuilding {

    private static final MMLogger logger = MMLogger.create(BuildingEntity.class);

    private Building building;

    private static final int LOC_BASE = 0;

    public static final String[] HIT_LOCATION_NAMES = { "building" };

    private static final String[] LOCATION_ABBREVIATIONS = { "BLDG" };
    private static final String[] LOCATION_NAMES = { "BLDG" };

    private static final int[] CRITICAL_SLOTS = new int[] { 100 };

    public BuildingEntity(BuildingType type, int bldgClass) {
        super();
        building = new Building(type, bldgClass, getId(), Terrains.BUILDING);

        initializeInternal(0, LOC_BASE);
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
        return 1;
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
        return LOCATION_NAMES;
    }

    @Override
    public String[] getLocationAbbreviations() {
        return LOCATION_ABBREVIATIONS;
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

    @Override
    public boolean hasCFIn(Coords coords) {
        return building.hasCFIn(coords);
    }

    @Override
    public Enumeration<Coords> getCoords() {
        return building.getCoords();
    }

    @Override
    public List<Coords> getCoordsList() {
        return building.getCoordsList();
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
        return building.getBasementCollapsed(coords);
    }

    @Override
    public void collapseBasement(Coords coords, Board board, Vector<Report> vPhaseReport) {
        building.collapseBasement(coords, board, vPhaseReport);
    }

    @Override
    public boolean rollBasement(Coords coords, Board board, Vector<Report> vPhaseReport) {
        return building.rollBasement(coords, board, vPhaseReport);
    }

    @Override
    public int getCurrentCF(Coords coords) {
        return building.getCurrentCF(coords);
    }

    @Override
    public int getPhaseCF(Coords coords) {
        return building.getPhaseCF(coords);
    }

    @Override
    public int getArmor(Coords coords) {
        return building.getArmor(coords);
    }

    @Override
    public void setCurrentCF(int cf, Coords coords) {
        building.setCurrentCF(cf, coords);
    }

    @Override
    public void setPhaseCF(int cf, Coords coords) {
        building.setPhaseCF(cf, coords);
    }

    @Override
    public void setArmor(int a, Coords coords) {
        building.setArmor(a, coords);
    }

    @Override
    public String getName() {
        return building.getName();
    }

    @Override
    public boolean isBurning(Coords coords) {
        return building.isBurning(coords);
    }

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
    public IBuilding getBuilding() {
        return building;
    }
}
