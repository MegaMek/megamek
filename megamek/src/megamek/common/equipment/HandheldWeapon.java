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


package megamek.common.equipment;

import java.io.Serial;
import java.util.Vector;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.cost.HandheldWeaponCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.UnitType;
import megamek.common.util.RoundWeight;

public class HandheldWeapon extends Entity {

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

    private static final TechAdvancement ADVANCEMENT = new TechAdvancement(TechBase.ALL).setAdvancement(3055, 3083)
          .setApproximate(false, true)
          .setPrototypeFactions(Faction.FS, Faction.LC)
          .setProductionFactions(Faction.FS, Faction.LC)
          .setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E)
          .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return ADVANCEMENT;
    }

    public static final int LOC_GUN = 0;
    private static final String[] LOCATION_NAMES = new String[] { "Gun" };
    private static final String[] LOCATION_ABBREVIATIONS = new String[] { "GUN" };

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
        return LOCATION_ABBREVIATIONS;
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
    public String getMovementString(EntityMovementType movementType) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public String getMovementAbbr(EntityMovementType movementType) {
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
    public void autoSetInternal() {
        initializeInternal(0, LOC_GUN);
    }

    @Override
    public int getWeaponArc(int weaponNumber) {
        throw new UnsupportedOperationException("Construction only.");
    }

    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        throw new UnsupportedOperationException("Construction only.");
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
}
