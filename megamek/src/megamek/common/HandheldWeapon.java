/*
 * MegaMek - Copyright (C) 2025 The MegaMek Team
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

import java.io.Serial;
import java.util.Vector;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.cost.HandheldWeaponCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.logging.MMLogger;

public class HandheldWeapon extends Entity {
    private static final MMLogger logger = MMLogger.create(HandheldWeapon.class);

    @Serial
    private static final long serialVersionUID = 5872304985723094857L;

    public HandheldWeapon() {
        super();
        setArmorType(MiscType.T_ARMOR_STANDARD);
        setArmorTechLevel(TechConstants.T_INTRO_BOXSET);
    }

    @Override
    public int getUnitType() {
        return UnitType.HANDHELD_WEAPON;
    }

    private static final TechAdvancement ADVANCEMENT = new TechAdvancement(TECH_BASE_ALL).setAdvancement(3055, 3083)
                                                             .setApproximate(false, true)
                                                             .setPrototypeFactions(F_FS, F_LC)
                                                             .setProductionFactions(F_FS, F_LC)
                                                             .setTechRating(RATING_D)
                                                             .setAvailability(RATING_E, RATING_E, RATING_F, RATING_E)
                                                             .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return ADVANCEMENT;
    }

    public static final int LOC_GUN = 0;
    private static final String[] LOCATION_NAMES = new String[] { "Gun" };
    private static final String[] LOCATION_ABBRS = new String[] { "GUN" };

    @Override
    public int locations() {
        return LOCATION_NAMES.length;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public boolean isValidSecondaryFacing(int dir) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public int clipSecondaryFacing(int dir) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public String getMovementString(EntityMovementType mtype) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public String getMovementAbbr(EntityMovementType mtype) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public HitData getTransferLocation(HitData hit) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public int getDependentLocation(int loc) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public boolean hasRearArmor(int loc) {
        return false;
    }

    @Override
    public void autoSetInternal() {
        initializeInternal(0, LOC_GUN);
    }

    @Override
    public int getWeaponArc(int wn) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public int getHeatCapacity(boolean radicalHeatSink) {
        return DOES_NOT_TRACK_HEAT;
    }

    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    @Override
    public int getEngineCritHeat() {
        return 0;
    }

    private static final int[] NUM_OF_SLOTS = new int[] { 25 };

    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    @Override
    public int getGenericBattleValue() {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public Vector<Report> victoryReport() {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public int getMaxElevationChange() {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return HandheldWeaponCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public boolean doomedInExtremeTemp() {
        return false;
    }

    @Override
    public boolean doomedInVacuum() {
        return false;
    }

    @Override
    public boolean doomedOnGround() {
        return false;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return false;
    }

    @Override
    public boolean doomedInSpace() {
        return false;
    }

    @Override
    public boolean isNuclearHardened() {
        // Genuinely I have no idea
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public int getTotalCommGearTons() {
        return 0;
    }

    @Override
    public int getEngineHits() {
        return 0;
    }

    @Override
    public String getLocationDamage(int loc) {
        return "";
    }

    @Override
    public boolean isCrippled() {
        return getArmor(LOC_GUN) == 0;
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        return getArmor(LOC_GUN) == 0;
    }

    @Override
    public boolean isDmgHeavy() {
        return getArmor(LOC_GUN) == 0;
    }

    @Override
    public boolean isDmgModerate() {
        return getArmor(LOC_GUN) == 0;
    }

    @Override
    public boolean isDmgLight() {
        return getArmor(LOC_GUN) == 0;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_HANDHELD_WEAPON;
    }

    @Override
    public boolean isHandheldWeapon() {
        return true;
    }

    @Override
    public double getArmorWeight() {
        return RoundWeight.nextHalfTon(getOArmor(LOC_GUN) / 16.0);
    }

    @Override
    public void clearInitiative(boolean bUseInitComp) {

    }
}
