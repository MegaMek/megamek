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
import megamek.common.compute.Compute;
import megamek.common.cost.HandheldWeaponCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.moves.MoveStep;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.UnitType;
import megamek.common.util.RoundWeight;
import megamek.server.totalWarfare.TWGameManager;

public class HandheldWeapon extends Entity {

    @Serial
    private static final long serialVersionUID = 5872304985723094857L;

    public HandheldWeapon() {
        super();
        setArmorType(MiscType.T_ARMOR_STANDARD);
        setArmorTechLevel(TechConstants.T_INTRO_BOX_SET);
    }

    @Override
    public CrewType defaultCrewType() {
        // Handheld Weapons don't have a crew
        return CrewType.NONE;
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
    public String[] getLocationAbbreviations() {
        return LOCATION_ABBREVIATIONS;
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return false;
    }

    @Override
    public boolean isValidSecondaryFacing(int dir) {
        return false;
    }

    @Override
    public int clipSecondaryFacing(int dir) {
        return Compute.ARC_FORWARD;
    }

    @Override
    public String getMovementString(EntityMovementType movementType) {
        return "None";
    }

    @Override
    public String getMovementAbbr(EntityMovementType movementType) {
        return "N";
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        return new HitData(LOC_GUN);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return new HitData(LOC_GUN);
    }

    @Override
    public HitData getTransferLocation(HitData hit) {
        return hit;
    }

    @Override
    public void autoSetInternal() {
        initializeInternal(0, LOC_GUN);
    }

    @Override
    public int getWeaponArc(int weaponNumber) {
        return Compute.ARC_FORWARD;
    }

    /**
     * Returns the primary facing, or -1 if n/a
     */
    @Override
    public int getFacing() {
        if (isTransported() && getGame() != null) {
            Entity carryingEntity = getGame().getEntity(getTransportId());
            if (carryingEntity != null && carryingEntity.getDistinctCarriedObjects().contains(this)) {
                return carryingEntity.getSecondaryFacing();
            }
        }
        return super.getFacing();
    }

    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        return false;
    }

    private static final int[] NUM_OF_SLOTS = new int[] { 25 };

    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    @Override
    public int getGenericBattleValue() {
        return calculateBattleValue();
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7035);
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(getCrew().getDescVector(false));
        r = new Report(7070, Report.PUBLIC);
        r.add(getKillNumber());
        vDesc.addElement(r);

        if (isDestroyed()) {
            Entity killer = game.getEntity(killerId);
            if (killer == null) {
                killer = game.getOutOfGameEntity(killerId);
            }
            if (killer != null) {
                r = new Report(7072, Report.PUBLIC);
                r.addDesc(killer);
            } else {
                r = new Report(7073, Report.PUBLIC);
            }
            vDesc.addElement(r);
        } else if (getCrew().isEjected()) {
            r = new Report(7071, Report.PUBLIC);
            vDesc.addElement(r);
        }
        r.newlines = 2;

        return vDesc;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        return roll;
    }

    @Override
    public int getMaxElevationChange() {
        return 0;
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return HandheldWeaponCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public boolean isNuclearHardened() {
        // Genuinely I have no idea
        return false;
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
        return (getOArmor(LOC_GUN)) > 0 && (double) getArmor(LOC_GUN) / getOArmor(LOC_GUN) <= 0.5;
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        return isCrippled();
    }

    @Override
    public boolean isDmgHeavy() {
        return (getOArmor(LOC_GUN)) > 0 && (double) getArmor(LOC_GUN) / getOArmor(LOC_GUN) < 0.5;
    }

    @Override
    public boolean isDmgModerate() {
        return (getOArmor(LOC_GUN)) > 0 && (double) getArmor(LOC_GUN) / getOArmor(LOC_GUN) < 0.75;
    }

    @Override
    public boolean isDmgLight() {
        return (getOArmor(LOC_GUN)) > 0 && (double) getArmor(LOC_GUN) / getOArmor(LOC_GUN) < 0.9;
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
    public boolean isCarryableObject() {
        return true;
    }

    /**
     * Returns true if the carryable object is able to be picked up. Units must be hull down to pick up other units,
     * unless the unit is tall. Airborne aeros cannot be grabbed either.
     *
     * @param isCarrierHullDown is the unit that's picking this up hull down, or otherwise able to pick up ground-level
     *                          objects
     *
     * @return true if the object can be picked up, false if it cannot
     */
    @Override
    public boolean canBePickedUp(boolean isCarrierHullDown) {
        return true;
    }

    /**
     * What entity is using this weapon to attack?
     *
     * @return entity carrying this weapon, or the entity itself
     */
    @Override
    public Entity getAttackingEntity() {
        return isTransported() ? game.getEntity(getTransportId()) : super.getAttackingEntity();
    }

    @Override
    public void processPickupStep(MoveStep step, Integer cargoPickupLocation, TWGameManager gameManager,
          Entity entityPickingUpTarget, EntityMovementType overallMoveType) {
        processPickupStepEntity(step, cargoPickupLocation, gameManager, entityPickingUpTarget);
    }

    @Override
    public CarriedObjectDamageAllocation getCarriedObjectDamageAllocation() {
        return CarriedObjectDamageAllocation.ARM_HIT;
    }

    @Override
    public int targetForArmHitToHitCarriedObject() {
        return 6;
    }

    /**
     * An entity is eligible for firing if it's not taking some kind of action that prevents it from firing, such as a
     * full-round physical attack or sprinting.
     */
    @Override
    public boolean isEligibleForFiring() {
        return false;
    }

    /**
     * Pretty much anybody's eligible for movement. If the game option is toggled on, inactive and immobile entities are
     * not eligible. OffBoard units are always ineligible
     *
     * @return whether the entity is allowed to move
     */
    @Override
    public boolean isEligibleForMovement() {
        return false;
    }

    @Override
    public boolean isEligibleForOffboard() {
        return false;
    }

    /**
     * Check if the entity has any valid targets for physical attacks.
     */
    @Override
    public boolean isEligibleForPhysical() {
        return false;
    }

    @Override
    public boolean isEligibleForTargetingPhase() {
        return false;
    }

    /**
     * @return True if this Entity is eligible to pre-designate hexes as auto-hits. Per TacOps pg 180, if a player has
     *       off board artillery they get 5 pre-designated hexes per map sheet.
     */
    @Override
    public boolean isEligibleForArtyAutoHitHexes() {
        return false;
    }
}
