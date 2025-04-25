/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *           Copyright (C) 2005 Mike Gratton <mike@vee.net>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.cost.CostCalculator;
import megamek.common.enums.AimingMode;
import megamek.logging.MMLogger;

import java.io.Serial;
import java.util.Optional;

/**
 * A building with weapons fitted and, optionally, a turret. Taharqa: I am completely re-writing this entity to bring it
 * up to code with TacOps rules GunEmplacements will not simply be the weapon loadouts that can be attached to
 * buildings. They will not be targetable in game, but will be destroyed if their building hex is reduced.
 */
public class GunEmplacement extends Tank {
    @Serial
    private static final long serialVersionUID = 8561738092216598248L;
    private static final MMLogger logger = MMLogger.create(GunEmplacement.class);

    // locations
    public static final int LOC_GUNS = 0;

    public static final String[] HIT_LOCATION_NAMES = { "guns" };

    private static final int[] CRITICAL_SLOTS = new int[] { 0 };
    private static final String[] LOCATION_ABBRS = { "GUN" };
    private static final String[] LOCATION_NAMES = { "GUNS" };

    private static final TechAdvancement TA_GUN_EMPLACEMENT = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_B).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.INTRO);

    public static final TechAdvancement TA_LIGHT_BUILDING = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.INTRO);

    private int initialBuildingCF;
    private int initialBuildingArmor;

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return TA_GUN_EMPLACEMENT;
    }

    public GunEmplacement() {
        initializeInternal(IArmorState.ARMOR_NA, LOC_GUNS);
        // give it an engine just to avoid NPE on calls to Tank
        setEngine(new Engine(0, Engine.NORMAL_ENGINE, Engine.TANK_ENGINE));
    }

    @Override
    public int getUnitType() {
        return UnitType.GUN_EMPLACEMENT;
    }

    public boolean isTurret() {
        return !hasNoTurret();
    }

    @Override
    public boolean isImmobile() {
        return true;
    }

    /**
     * Our gun emplacements do not support dual turrets at this time
     */
    @Override
    public boolean hasNoDualTurret() {
        return true;
    }

    @Override
    public boolean isEligibleForMovement() {
        return false;
    }

    @Override
    public String getMovementString(EntityMovementType mtype) {
        return "Not possible!";
    }

    @Override
    public String getMovementAbbr(EntityMovementType mtype) {
        return "!";
    }

    @Override
    public boolean isLocationProhibited(Coords c, int testBoardId, int currElevation) {
        if (!game.hasBoardLocation(c, testBoardId)) {
            return true;
        }

        Hex hex = game.getHex(c, testBoardId);

        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }
        // gun emplacements must be placed on a building
        return !hex.containsTerrain(Terrains.BUILDING);

    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        return 0; // Overridden for performance and to keep it from being made non-zero by any
                  // rule
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        return 0; // Overridden for performance and to keep it from being made non-zero by any
                  // rule
    }

    @Override
    public int getSprintMP(MPCalculationSetting mpCalculationSetting) {
        return 0; // Overridden for performance and to keep it from being made non-zero by any
                  // rule
    }

    @Override
    public int getJumpMP(MPCalculationSetting mpCalculationSetting) {
        return 0; // Overridden for performance and to keep it from being made non-zero by any
                  // rule
    }

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    @Override
    public int locations() {
        return 1;
    }

    @Override
    public int getWeaponArc(int weaponId) {
        return isTurret() ? Compute.ARC_TURRET : Compute.ARC_FORWARD;
    }

    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        return isTurret();
    }

    @Override
    public int sideTable(Coords src) {
        return ToHitData.SIDE_FRONT;
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
            int cover) {
        return rollHitLocation(table, side);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return new HitData(LOC_GUNS, false, HitData.EFFECT_NONE);
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        return prd;
    }

    @Override
    public int[] getNoOfSlots() {
        return CRITICAL_SLOTS;
    }

    @Override
    public int getHeatCapacity() {
        return DOES_NOT_TRACK_HEAT;
    }

    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    @Override
    public void autoSetInternal() {
        initializeInternal(0, LOC_GUNS);
    }

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

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        CostCalculator.addNoReportNote(calcReport, this);
        return 0;
    }

    @Override
    public boolean doomedInVacuum() {
        return false;
    }

    @Override
    public void addEquipment(Mounted<?> mounted, int loc, boolean rearMounted)
            throws LocationFullException {
        super.addEquipment(mounted, loc, rearMounted);
        // Add the piece equipment to our slots.
        addCritical(loc, new CriticalSlot(mounted));
    }

    @Override
    public int getTotalCommGearTons() {
        return getExtraCommGearTons();
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        if (checkCrew && (null != getCrew()) && getCrew().isDead()) {
            logger.debug(getDisplayName() + " CRIPPLED: Crew dead.");
            return true;
        } else if (isMilitary() && !hasViableWeapons()) {
            logger.debug(getDisplayName() + " CRIPPLED: no viable weapons left.");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isDmgHeavy() {
        long inoperableWeapons = inoperableWeaponCount();
        return inoperableWeapons != 0 && ((double) inoperableWeapons / getTotalWeaponList().size()) >= 0.75;
    }

    @Override
    public boolean isDmgLight() {
        long inoperableWeapons = inoperableWeaponCount();
        return inoperableWeapons != 0 && ((double) inoperableWeapons / getTotalWeaponList().size()) >= 0.25;
    }

    @Override
    public boolean isDmgModerate() {
        long inoperableWeapons = inoperableWeaponCount();
        return inoperableWeapons != 0 && ((double) inoperableWeapons / getTotalWeaponList().size()) >= 0.5;
    }

    private long inoperableWeaponCount() {
        return getTotalWeaponList().stream().filter(Mounted::isCrippled).count();
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_TANK | Entity.ETYPE_GUN_EMPLACEMENT;
    }

    @Override
    public boolean hasEngine() {
        // TODO: Power generators and energy grid setup
        return false;
    }

    @Override
    public int getArmorType(int loc) {
        // this is a hack to get around the fact that gun emplacements don't even have armor
        return 0;
    }

    @Override
    public int getArmorTechLevel(int loc) {
        return TechConstants.T_INTRO_BOXSET;
    }

    @Override
    public boolean hasStealth() {
        return false;
    }

    /**
     * Sets the deployed flag.
     * Has the side effect of initializing building CF if deployed
     */
    @Override
    public void setDeployed(boolean deployed) {
        super.setDeployed(deployed);
        Optional<Building> occupiedBuilding = occupiedBuilding();
        initialBuildingCF = occupiedBuilding.map(bldg -> bldg.getCurrentCF(getPosition())).orElse(0);
        initialBuildingArmor = occupiedBuilding.map(bldg -> bldg.getArmor(getPosition())).orElse(0);
    }

    /**
     * @return The Building this Gun Emplacement is deployed onto, if any. Safe to call under any circumstances (game
     * may be null, may be undeployed etc).
     */
    private Optional<Building> occupiedBuilding() {
        if (game != null) {
            return game.getBuildingAt(getPosition(), getBoardId());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public double getArmorRemainingPercent() {
        return occupiedBuilding().map(this::armorPercentage).orElse(1d);
    }

    private double armorPercentage(Building building) {
        if (initialBuildingCF + initialBuildingArmor == 0) {
            // probably undeployed; avoid division by zero
            return 1;
        } else {
            return (building.getCurrentCF(getPosition()) + building.getArmor(getPosition()))
                         / ((double) (initialBuildingCF + initialBuildingArmor));
        }
    }

    /**
     * Gun emplacements don't have critical slots per se, so we
     * simply return 1 if the piece of equipment has been hit and 0 otherwise.
     */
    @Override
    public int getDamagedCriticals(int type, int index, int loc) {
        Mounted<?> m;
        if (type == CriticalSlot.TYPE_EQUIPMENT) {
            m = getEquipment(index);

            if (m != null && m.isHit()) {
                return 1;
            }
        }

        return 0;
    }

    @Override
    public int getGenericBattleValue() {
        // they have no weight so we use the mean across all GEs
        return 205;
    }

    @Override
    public boolean isSideLocation(int location) {
        return false;
    }
}
