/*
 * Copyright (c) 2022-2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.battlevalue;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.codeUtilities.MathUtility;
import megamek.common.*;
import megamek.common.equipment.ArmorType;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bayweapons.BayWeapon;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.AmmoType.*;

import java.util.*;
import java.util.function.Predicate;

/**
 * Base class for battle value calculators for all units. The subclasses implement overrides
 * as necessary for the bv calculation process that all unit types follow. To obtain the correct
 * BVCalculator, use {@link #getBVCalculator(Entity)}.
 */
public abstract class BVCalculator {

    protected final static String REAR = " (R)";
    protected final static String TURRET = " (T)";
    protected final static int NO_HEAT = Integer.MAX_VALUE;

    protected final Entity entity;
    protected double defensiveValue;
    protected double offensiveValue;
    protected CalculationReport bvReport;
    protected boolean ignoreC3;
    protected boolean ignoreSkill;
    protected Map<String, Double> ammoMap = new HashMap<>();
    protected List<String> keys = new ArrayList<>();
    protected Map<String, String> names = new HashMap<>();
    protected Map<String, Double> weaponsForExcessiveAmmo = new HashMap<>();
    protected int runMP;
    protected int jumpMP;
    protected int umuMP;
    protected boolean hasBlueShield;
    protected boolean hasTC;
    protected boolean isDrone;
    protected boolean frontAndRearDecided = false;
    protected boolean switchRearAndFront = false;
    protected boolean heatEfficiencyExceeded = false;
    protected double heatSum;
    private final Map<Mounted, Integer> collectedDefensiveEquipment = new HashMap<>();

    /** The unit's BV without any force adjustments */
    protected double baseBV = -1;

    /** The unit's BV with the TAG force bonus */
    protected double tagBV = -1;

    /** The unit's BV with the C3 force bonus and pilot skill adjustment */
    protected double adjustedBV = -1;

    BVCalculator(Entity entity) {
        this.entity = entity;
    }

    public static BVCalculator getBVCalculator(Entity entity) {
        if (entity instanceof Mech) {
            return new MekBVCalculator(entity);
        } else if (entity instanceof Protomech) {
            return new ProtoMekBVCalculator(entity);
        } else if (entity instanceof BattleArmor) {
            return new BattleArmorBVCalculator(entity);
        } else if (entity instanceof Infantry) {
            return new InfantryBVCalculator(entity);
        } else if (entity instanceof Warship) {
            return new WarShipBVCalculator(entity);
        } else if (entity instanceof Jumpship) {
            return new JumpShipBVCalculator(entity);
        } else if (entity instanceof Dropship) {
            return new DropShipBVCalculator(entity);
        } else if (entity instanceof Aero) {
            return new AeroBVCalculator(entity);
        } else if (entity instanceof GunEmplacement) {
            return new GunEmplacementBVCalculator(entity);
        } else { // Tank
            return new CombatVehicleBVCalculator(entity);
        }
    }

    /**
     * Calculate and return the current battle value of the entity of this calculator. Depending
     * on the parameters C3 bonuses and/or pilot skill may be removed from the calculation.
     *
     * @param ignoreC3 When true, the force bonus for C3 connections is not added.
     * @param ignoreSkill When true, the pilot skill (including MD) is not factored in.
     * @return The newly calculated battle value.
     */
    public int calculateBV(boolean ignoreC3, boolean ignoreSkill) {
        return calculateBV(ignoreC3, ignoreSkill, new DummyCalculationReport());
    }

    /**
     * Calculate and return the current battle value of the entity of this calculator. Depending
     * on the parameters C3 bonuses and/or pilot skill may be removed from the calculation. The
     * given report is filled in.
     *
     * @param ignoreC3 When true, the force bonus for C3 connections is not added.
     * @param ignoreSkill When true, the pilot skill (including MD) is not factored in.
     * @param bvReport The report to fill in with the calculation.
     * @return The newly calculated battle value.
     */
    public int calculateBV(boolean ignoreC3, boolean ignoreSkill, CalculationReport bvReport) {
        this.ignoreC3 = ignoreC3;
        this.ignoreSkill = ignoreSkill;
        calculateBaseBV(bvReport);
        adjustBV();
        return (int) Math.round(adjustedBV);
    }

    /**
     * Calculate and return the base battle value of the entity of this calculator. The base BV
     * does not include any force bonuses, i.e. external stores, C3, pilot skill and TAG bonuses.
     *
     * @return The newly calculated base unit battle value.
     */
    public int calculateBaseBV() {
        return calculateBaseBV(new DummyCalculationReport());
    }

    /**
     * Calculate and return the base battle value of the entity of this calculator. The base BV
     * does not include any force bonuses, i.e. external stores, C3, pilot skill and TAG bonuses.
     * The given report is filled in with the calculation.
     *
     * @param bvReport The report to fill in with the calculation.
     * @return The newly calculated base unit battle value.
     */
    public int calculateBaseBV(CalculationReport bvReport) {
        this.bvReport = bvReport;
        processBaseBV();
        return (int) Math.round(baseBV);
    }

    /**
     * Retrieves a previously calculated base battle value of the unit without re-calculating
     * it; see {@link #calculateBaseBV()}! This should only be used when it is certain
     * that the value is still correct. The base BV does not include any force bonuses.
     *
     * @return The stored base unit battle value.
     */
    public int retrieveBaseBV() {
        return (int) Math.round(baseBV);
    }

    /**
     * Retrieves a previously calculated battle value of the unit without re-calculating
     * it. This BV includes the Tag force bonus but no other force bonuses! it can be
     * used as a basis for calculating the C3 bonus without recalculating all units repeatedly.
     * This should only be used when it is certain that the value is still correct.
     *
     * @return The stored unit battle value including Tag bonus.
     */
    public int retrieveBVWithTag() {
        return (int) Math.round(tagBV);
    }

    /**
     * Retrieves a previously calculated full battle value of the unit without re-calculating
     * it; see {@link #calculateBV(boolean, boolean)}! This should only be used when it is certain
     * that the value is still correct. The full BV includes all force bonuses.
     *
     * @return The stored full unit battle value.
     */
    public int retrieveBV() {
        return (int) Math.round(adjustedBV);
    }

    /** Calculates the base unit BV (without force and pilot modifiers). */
    protected void processBaseBV() {
        processPreparation();

        if (entity.isCarcass()) {
            bvReport.addLine("Unit is destroyed.");
            baseBV = 0;
            return;
        }

        processCalculations();
    }

    protected void processPreparation() {
        reset();
        assembleAmmo();
        assembleMovementPoints();

        String header = "Battle Value Calculation for ";
        String fullName = entity.getChassis() + " " + entity.getModel();
        if (fullName.length() < 20) {
            bvReport.addHeader(header + fullName);
        } else if (fullName.length() < 50) {
            bvReport.addHeader(header);
            bvReport.addHeader(fullName);
        } else {
            bvReport.addHeader(header);
            bvReport.addHeader(entity.getChassis());
            bvReport.addHeader(entity.getModel());
        }
        bvReport.addEmptyLine();
    }

    protected void processCalculations() {
        bvReport.addLine("Effective MP:", "R: " + runMP + ", J: " + jumpMP + ", U: " + umuMP, "");
        bvReport.addEmptyLine();
        bvReport.addSubHeader("Defensive Battle Rating:");
        processDefensiveValue();
        bvReport.addEmptyLine();
        bvReport.addSubHeader("Offensive Battle Rating:");
        processOffensiveValue();
        processSummarize();
    }

    protected void processDefensiveValue() {
        processArmor();
        processStructure();
        processDefensiveEquipment();
        processExplosiveEquipment();
        processTypeModifier();
        processDefensiveFactor();
    }

    protected void processOffensiveValue() {
        determineFront();
        processWeapons();
        processAmmo();
        processOffensiveEquipment();
        processWeight();
        processSpeedFactor();
        processOffensiveTypeModifier();
    }

    protected void reset() {
        hasBlueShield = entity.hasWorkingMisc(MiscType.F_BLUE_SHIELD);
        hasTC = entity.hasTargComp();
        isDrone = entity.hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM);
        frontAndRearDecided = false;
        switchRearAndFront = false;
        defensiveValue = 0;
        offensiveValue = 0;
        baseBV = 0;
        adjustedBV = 0;
        ammoMap.clear();
        keys.clear();
        names.clear();
        weaponsForExcessiveAmmo.clear();
        heatEfficiencyExceeded = false;
        heatSum = 0;
        collectedDefensiveEquipment.clear();
    }

    protected void assembleMovementPoints() {
        setRunMP();
        setJumpMP();
        setUmuMP();
    }

    /**
     * Sets the running MP as used for battle value calculations. This value should not factor
     * in gravity or weather (as these aren't well visible in the calculation, may change over
     * the course of a battle and aren't available in MHQ).
     * It also should not factor in player-controlled transients such as
     * cargo, trailers, bombs, heat, movement mode changes (LAM, WiGE, QuadVees), grounded/landed
     * status (Aero) as these would also change BV in battle in strange ways. Also, it should
     * ignore advanced rules such as TO Infantry Fast Movement to prevent base BV values
     * different from those on the MUL.
     *
     * It should factor in intransient modifiers such as TSM, modular or hardened armor as well as
     * damage to the unit (engine hits, motive damage, immobile status).
     */
    protected void setRunMP() {
        runMP = entity.getRunMP(MPCalculationSetting.BV_CALCULATION);
    }

    /**
     * Sets the jumping MP as used for battle value calculations. Here the same rules apply as with
     * {@link #setRunMP()}.
     */
    protected void setJumpMP() {
        jumpMP = entity.getJumpMP(MPCalculationSetting.BV_CALCULATION);
    }

    /**
     * Sets the UMU MP as used for battle value calculations. Here the same rules apply as with
     * {@link #setRunMP()}.
     */
    protected void setUmuMP() {
        umuMP = entity.getActiveUMUCount();
    }

    protected void processTypeModifier() { }

    /**
     * Returns true when the given location is valid for armor BV calculations. Returns
     * true by default. Override to exclude locations, e.g. hull on Aeros.
     *
     * @param location The location to check
     * @return True when the given location must be considered for Armor BV
     */
    protected boolean validArmorLocation(int location) {
        return true;
    }

    protected void processArmor() {
        double totalArmorBV = 0;
        // Units with patchwork armor or Harjel II/III calculate and list every location separately
        if (entity.hasPatchworkArmor() || entity.hasWorkingMisc(MiscType.F_HARJEL_II)
                || entity.hasWorkingMisc(MiscType.F_HARJEL_III)) {
            bvReport.addLine("Armor:", "", "");
            for (int loc = 0; loc < entity.locations(); loc++) {
                if (!validArmorLocation(loc)) {
                    continue;
                }
                double armorMultiplier = armorMultiplier(loc);
                double torsoMountedCockpit = addTorsoMountedCockpit();

                // Rear Armor
                int rearArmor = entity.hasRearArmor(loc) ? entity.getArmor(loc, true) : 0;
                int armor = entity.getArmor(loc) + (entity.hasRearArmor(loc) ? entity.getArmor(loc, true) : 0);
                String calculation = entity.getArmor(loc) + "";
                calculation += entity.hasRearArmor(loc) ? " + " + rearArmor + " (R)" : "";

                // Modular Armor
                int modularArmor = 0;
                for (Mounted mounted : entity.getMisc()) {
                    if (mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR) && (mounted.getLocation() == loc)) {
                        modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
                    }
                }
                if (modularArmor > 0) {
                    calculation += " + " + modularArmor + " (Mod.)";
                }

                // BAR Rating
                double barRating = entity.getBARRating(loc) / 10.0;

                if (((barRating != 1) || (armorMultiplier != 1)) && ((rearArmor != 0) || (modularArmor != 0))) {
                    calculation = "[" + calculation + "] ";
                }

                if (armorMultiplier != 1) {
                    calculation += " x " + formatForReport(armorMultiplier) + armorMultiplierText(loc);
                }

                double armorBV = (armor + modularArmor + torsoMountedCockpit) * armorMultiplier * barRating;
                totalArmorBV += armorBV;
                String type = "- " + EquipmentType.getArmorTypeName(entity.getArmorType(loc))
                        + " (" + entity.getLocationAbbr(loc) + ")";
                bvReport.addLine(type, "+ " + calculation + " = " + formatForReport(totalArmorBV), "");
            }
            defensiveValue += totalArmorBV * armorFactor();
            bvReport.addLine("Armor BV:",
                    formatForReport(totalArmorBV) + " x " + formatForReport(armorFactor()),
                    "= " + formatForReport(defensiveValue));
        } else {
            // Units without Patchwork armor can use the (simpler) total armor value
            int modularArmor = entity.getMisc().stream()
                    .filter(m -> m.getType().hasFlag(MiscType.F_MODULAR_ARMOR))
                    .mapToInt(m -> m.getBaseDamageCapacity() - m.getDamageTaken())
                    .sum();
            double armorMultiplier = armorMultiplier(0);
            double barRating = entity.getBARRating(0) / 10.0;
            double torsoMountedCockpit = addTorsoMountedCockpit();

            double totalArmor = entity.getTotalArmor() + modularArmor + torsoMountedCockpit;
            totalArmorBV = totalArmor * armorMultiplier * barRating;
            String calculation = entity.getTotalArmor() + "";
            calculation += (modularArmor > 0) ? " + " + modularArmor + " (Mod.)" : "";
            calculation += (torsoMountedCockpit > 0) ? " + " + formatForReport(torsoMountedCockpit) + " (Torso-m. Cockpit)" : "";
            if (totalArmor > entity.getTotalArmor()) {
                calculation = "(" + calculation + ")";
            }

            calculation += " x " + formatForReport(armorFactor());
            calculation += (armorMultiplier != 1) ?
                    " x " + formatForReport(armorMultiplier) + " ("
                            + ArmorType.forEntity(entity).getName() + ")" : "";
            calculation += (barRating != 1) ?
                    " x " + formatForReport(barRating) + " (BAR)" : "";
            defensiveValue += totalArmorBV * armorFactor();
            bvReport.addLine("Armor:", calculation, "= " + formatForReport(defensiveValue));
        }
    }

    /** @return The base factor to multiply armor by, i.e. 25 for capital aerosapce and 2.5 for all others. */
    protected double armorFactor() {
        return 2.5;
    }

    protected String equipmentDescriptor(Mounted mounted) {
        if (mounted.getType() instanceof WeaponType) {
            String descriptor = mounted.getType().getShortName() + " (" + entity.getLocationAbbr(mounted.getLocation()) + ")";
            if (mounted.isMechTurretMounted()) {
                descriptor += TURRET;
            }
            if (mounted.isRearMounted() || isRearFacingVGL(mounted)) {
                descriptor += REAR;
            }
            return descriptor;
        } else if ((mounted.getType() instanceof MiscType)
                && ((MiscType) mounted.getType()).isVibroblade()) {
            return mounted.getType().getShortName() + " (" + entity.getLocationAbbr(mounted.getLocation()) + ")";
        } else if (mounted.getType() instanceof AmmoType) {
            String shortName = mounted.getType().getShortName();
            return shortName + (shortName.contains("Ammo") ? "" : " Ammo");
        } else {
            return mounted.getType().getShortName();
        }
    }

    protected double addTorsoMountedCockpit() {
        return 0;
    }

    protected void processStructure() {
        String calculation = "+ " + entity.getTotalInternal() + " x 1.5";
        String typeModifier = "";
        double typeMultiplier = 1.0;
        if ((entity.getStructureType() == EquipmentType.T_STRUCTURE_INDUSTRIAL)
                || (entity.getStructureType() == EquipmentType.T_STRUCTURE_COMPOSITE)) {
            typeMultiplier = 0.5;
            typeModifier = (entity.getStructureType() == EquipmentType.T_STRUCTURE_COMPOSITE) ? " (Comp." : " (Ind.";
        } else if (entity.getStructureType() == EquipmentType.T_STRUCTURE_REINFORCED) {
            typeMultiplier = 2.0;
            typeModifier = "(Reinf.";
        }
        if (hasBlueShield) {
            typeMultiplier += 0.2;
            typeModifier += typeModifier.isBlank() ? " (Blue Shield)" : ", Blue Shield)";
        }
        if (typeMultiplier != 1) {
            calculation += " x " + formatForReport(typeMultiplier) + typeModifier;
        }

        defensiveValue += entity.getTotalInternal() * 1.5 * typeMultiplier;
        bvReport.addLine("Internal Structure:", calculation, "= " + formatForReport(defensiveValue));
    }

    protected boolean countsAsDefensiveEquipment(Mounted equipment) {
        if (equipment.isDestroyed() || equipment.isWeaponGroup()
                || (equipment.getType() instanceof BayWeapon)) {
            return false;
        }

        EquipmentType eType = equipment.getType();
        if (eType instanceof WeaponType) {
            return eType.hasFlag(WeaponType.F_AMS)
                    || eType.hasFlag(WeaponType.F_M_POD)
                    || eType.hasFlag(WeaponType.F_B_POD)
                    || (((WeaponType) eType).getAtClass() == WeaponType.CLASS_SCREEN);
        } else if (eType instanceof MiscType) {
            return eType.hasFlag(MiscType.F_ECM)
                    || eType.hasFlag(MiscType.F_BAP)
                    || eType.hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)
                    || eType.hasFlag(MiscType.F_VIRAL_JAMMER_HOMING)
                    || eType.hasFlag(MiscType.F_AP_POD)
                    || eType.hasFlag(MiscType.F_MASS)
                    || eType.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                    || eType.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || eType.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                    || eType.hasFlag(MiscType.F_BULLDOZER)
                    || eType.hasFlag(MiscType.F_CHAFF_POD)
                    || eType.hasFlag(MiscType.F_HARJEL_II)
                    || eType.hasFlag(MiscType.F_HARJEL_III)
                    || eType.hasFlag(MiscType.F_SPIKES)
                    || eType.hasFlag(MiscType.F_MINESWEEPER)
                    || ((MiscType) eType).isShield();
        } else {
            return false;
        }
    }

    protected void processDefensiveEquipment() {
        bvReport.startTentativeSection();
        bvReport.addLine("Defensive Equipment:", "", "");
        double amsBV = 0;
        double amsAmmoBV = 0;
        double screenBV = 0;
        double screenAmmoBV = 0;
        boolean hasDefensiveEquipment = false;
        for (Mounted ammo : entity.getAmmo()) {
            if (ammo.getUsableShotsLeft() == 0) {
                continue;
            }
            // Ammo may be loaded in multi-ton increments on large aerospace
            AmmoType ammoType = (AmmoType) ammo.getType();
            int ratio = 1;
            if (ammoType.getShots() > 0) {
                ratio = Math.max(1, ammo.getUsableShotsLeft() / ammoType.getShots());
            }

            if ((ammoType.getAmmoType() == T_AMS) || (ammoType.getAmmoType() == T_APDS)) {
                amsAmmoBV += ammoType.getBV(entity) * ratio;
            } else if (ammoType.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) {
                screenAmmoBV += ammoType.getBV(entity) * ratio;
            }
        }

        for (Mounted equipment : entity.getEquipment()) {
            if (countsAsDefensiveEquipment(equipment)) {
                Mounted key = collectedDefensiveEquipment.keySet().stream()
                        .filter(p -> equipment.getType() == p.getType()).findFirst().orElse(equipment);
                collectedDefensiveEquipment.merge(key, 1, Integer::sum);
            }
        }

        for (Map.Entry<Mounted, Integer> equipmentEntry : collectedDefensiveEquipment.entrySet()) {
            Mounted equipment = equipmentEntry.getKey();
            EquipmentType eType = equipment.getType();
            double equipmentBV = eType.getBV(entity);
            if (eType instanceof MiscType) {
                equipmentBV = ((MiscType) eType).getBV(entity, equipment.getLocation());
            }
            String multiplier = (equipmentEntry.getValue() > 1) ? equipmentEntry.getValue() + " x " : "";
            defensiveValue += equipmentBV * equipmentEntry.getValue();
            String calculation = ((equipmentBV > 0) ? "+ " : "- ");
            calculation += multiplier + formatForReport(Math.abs(equipmentBV));
            bvReport.addLine("- " + multiplier + equipmentDescriptor(equipment), calculation,
                    "= " + formatForReport(defensiveValue));
            hasDefensiveEquipment = true;

            if (eType instanceof WeaponType) {
                WeaponType wtype = (WeaponType) eType;
                if (wtype.hasFlag(WeaponType.F_AMS)
                        && ((wtype.getAmmoType() == T_AMS) || (wtype.getAmmoType() == T_APDS))) {
                    amsBV += eType.getBV(entity) * equipmentEntry.getValue();
                }
            }
            if ((eType instanceof WeaponType)
                    && (((WeaponType) eType).getAtClass() == WeaponType.CLASS_SCREEN)) {
                screenBV += eType.getBV(entity) * equipmentEntry.getValue();
            }
        }

        if (amsAmmoBV > 0) {
            double nonExcessiveBV = Math.min(amsBV, amsAmmoBV);
            String calculation = "+ " + formatForReport(nonExcessiveBV) + ((amsAmmoBV > amsBV) ? " (Excessive)" : "");
            defensiveValue += nonExcessiveBV;
            bvReport.addLine("- AMS Ammo", calculation, "= " + formatForReport(defensiveValue));
            hasDefensiveEquipment = true;
        }
        if (screenAmmoBV > 0) {
            double nonExcessiveBV = Math.min(screenBV, screenAmmoBV);
            String calculation = "+ " + formatForReport(nonExcessiveBV) + ((screenAmmoBV > screenBV) ? " (Excessive)" : "");
            defensiveValue += nonExcessiveBV;
            bvReport.addLine("- Screen Launcher Ammo", calculation, "= " + formatForReport(defensiveValue));
            hasDefensiveEquipment = true;
        }
        bvReport.finalizeTentativeSection(hasDefensiveEquipment);
    }

    protected void processExplosiveEquipment() { }

    protected void processDefensiveFactor() {
        int tmmRunning = getRunningTMM();
        int tmmJumping = getJumpingTMM();
        int tmmUmu = getUmuTMM();
        bvReport.addLine("TMMs:", tmmRunning + " (R), " + tmmJumping + " (J), " + tmmUmu + " (U)", "");
        double tmmFactor = tmmFactor(tmmRunning, tmmJumping, tmmUmu);
        bvReport.addLine("Defensive Factor:",
                formatForReport(defensiveValue) + " x " + formatForReport(tmmFactor),
                "= " + formatForReport(tmmFactor * defensiveValue));
        defensiveValue *= tmmFactor;
    }

    protected int getRunningTMM() {
        if (runMP == 0) {
            return 0;
        } else {
            return Compute.getTargetMovementModifier(runMP, false, false, entity.getGame()).getValue();
        }
    }

    protected int getJumpingTMM() {
        if (jumpMP == 0) {
            return 0;
        } else {
            return Compute.getTargetMovementModifier(jumpMP, true, false, entity.getGame()).getValue();
        }
    }

    protected int getUmuTMM() {
        if (umuMP == 0) {
            return 0;
        } else {
            return Compute.getTargetMovementModifier(umuMP, false, false, entity.getGame()).getValue();
        }
    }

    protected double tmmFactor(int tmmRunning, int tmmJumping, int tmmUmu) {
        return 1 + (Math.max(tmmRunning, Math.max(tmmJumping, tmmUmu)) / 10.0);
    }

    protected void determineFront() {
        Predicate<Mounted> frontFilter = frontWeaponFilter();
        Predicate<Mounted> rearFilter = rearWeaponFilter();
        double weaponsBVFront = processWeaponSection(false, frontFilter, false);
        double weaponsBVRear = processWeaponSection(false, rearFilter, false);
        switchRearAndFront = weaponsBVFront < weaponsBVRear;
        if (switchRearAndFront) {
            bvReport.addLine("Front BV < Rear BV", "Switching Front and Rear", "");
        }
        frontAndRearDecided = true;
    }

    protected void processWeapons() {
        bvReport.addLine("Weapons:", "", "");
        double weaponBV = processWeaponSection(true, this::countAsOffensiveWeapon, true);
        if (weaponBV == 0) {
            bvReport.addLine("- None.", "", "0");
        }
    }

    protected boolean isRearFacingVGL(Mounted weapon) {
        // vehicular grenade launchers facing to the rear sides count for rear BV, too
        return weapon.getType().hasFlag(WeaponType.F_VGL) &&
             (weapon.getFacing() >= 2) && (weapon.getFacing() <= 4);
    }

    protected boolean isFrontFacingVGL(Mounted weapon) {
        // vehicular grenade launchers facing to the rear sides count for rear BV, too
        return weapon.getType().hasFlag(WeaponType.F_VGL) &&
                ((weapon.getFacing() == 1) || (weapon.getFacing() >= 5));
    }

    protected double processWeaponSection(boolean showInReport, Predicate<Mounted> weaponFilter,
                                          boolean addToOffensiveValue) {
        return entity.getEquipment().stream()
                .filter(this::countAsOffensiveWeapon)
                .filter(weaponFilter)
                .mapToDouble(weapon -> processWeapon(weapon, showInReport, addToOffensiveValue))
                .sum();
    }

    /**
     * Returns true when a weapon is to be counted and calculated as a rear weapon.
     * For units that deal with rear weapons this is usually true when the weapon is actually
     * rear-facing but may be false when weapon facing is reversed (when the front weapons BV
     * is smaller than the rear weapon BV).
     * This method should rely on the switchRearAndFront field to decide the return value.
     * switchRearAndFront is set before weapons are processed by the call to
     * {@link #determineFront()}.
     * By default, this returns false which is correct for units that do not deal with rear
     * weapons such as ProtoMeks.
     * This is overridden as necessary for units that deal with rear weapons (Mek, Aero, Tanks,
     * but not large aerospace that use arcs).
     *
     * @param weapon The Mounted equipment to check
     * @return True when the weapon is to be counted as if it was rear-facing
     */
    protected boolean isNominalRear(Mounted weapon) {
        return switchRearAndFront ^ rearWeaponFilter().test(weapon);
    }

    /**
     * Returns true when a weapon is to be counted and calculated as a "rear" arc weapon in
     * large aerospace units. The nominal rear arcs are those that are valued at 25% only.
     * By default, this returns false. This is overridden for large aerospace units.
     *
     * @param weapon The Mounted equipment to check
     * @return True when the weapon is to be counted as in a "rear" arc
     */
    protected boolean isNominalRearArc(Mounted weapon) {
        return false;
    }

    /**
     * @return True when rear and front weapon switching has been decided and
     * the given weapon is to be counted as rear-facing.
     */
    private boolean isDecidedAsNominalRear(Mounted weapon) {
        return frontAndRearDecided && isNominalRear(weapon);
    }

    /**
     * @return When true, will show individual weapon heat sums. Used in Meks, AF, CF, and SC.
     */
    protected boolean usesWeaponHeat() {
        return false;
    }

    /**
     * @return The multiplier for the arc that the given equipment is in (1, 0.5, or 0.25).
     * Overridden for large aerospace units. When not 1, the factor is shown in the report
     * and multiplied into the resulting BV.
     */
    protected double arcFactor(Mounted equipment) {
        return 1;
    }

    /**
     * Forwards to {@link #processWeapon(Mounted, boolean, boolean, int)} with a weaponCount
     * parameter of 1 (single weapon).
     */
    protected double processWeapon(Mounted weapon, boolean showInReport,
                                   boolean addToOffensiveValue) {
        return processWeapon(weapon, showInReport, addToOffensiveValue, 1);
    }

    /**
     * Determines the BV for one or more weapons of a single type which may include
     * a WeaponType, MiscType or AmmoType.
     * When showInReport is false, nothing is written to the report. Otherwise, a line
     * with the weapon's name and (if it has a WeaponType) location as well as the calculation
     * and modifiers is shown.
     *
     * @param weapon The Mounted to process - may include a WeaponType, MiscType or AmmoType
     * @param showInReport When true, will write a line for this weapon to the report.
     * @param weaponCount The number of this particular type of weapon (multiplies the BV)
     * @param addToOffensiveValue When true, will add the result to offensiveValue and show the result
     * @return The BV for this weapon
     */
    protected double processWeapon(Mounted<?> weapon, boolean showInReport,
                                   boolean addToOffensiveValue, int weaponCount) {
        double weaponBV = weapon.getType().getBV(entity);

        // MG Arrays need to sum up their linked MGs
        if ((weapon.getType() instanceof WeaponType) && weapon.getType().hasFlag(WeaponType.F_MGA)) {
            double mgBV = 0;
            for (int eqNum : weapon.getBayWeapons()) {
                Mounted<?> mg = entity.getEquipment(eqNum);
                if ((mg != null) && (!mg.isDestroyed())) {
                    mgBV += mg.getType().getBV(entity);
                }
            }
            weaponBV = mgBV * 0.67;
        }

        String multiplierText = (weaponCount > 1) ? weaponCount + " x " : "";
        String squadSupportDivisorText = "";
        double squadSupportDivisor = 1;
        if (entity instanceof BattleArmor && weapon.isSquadSupportWeapon()) {
            squadSupportDivisorText = " / " + ((BattleArmor) entity).getShootingStrength();
            squadSupportDivisor = ((BattleArmor) entity).getShootingStrength();
        }
        String calculation = "+ " + multiplierText + formatForReport(weaponBV) + squadSupportDivisorText;
        weaponBV *= weaponCount;
        weaponBV /= squadSupportDivisor;

        if (entity.hasFunctionalArmAES(weapon.getLocation())) {
            weaponBV *= 1.25;
            calculation += " x 1.25 (AES)";
        }

        if (isDecidedAsNominalRear(weapon)) {
            weaponBV /= 2;
            calculation += " x 0.5 (R)";
        }

        double arcFactor = arcFactor(weapon);
        if (arcFactor != 1) {
            weaponBV *= arcFactor;
            calculation += " x " + formatForReport(arcFactor) + " (Arc)";
        }

        if (isDrone) {
            weaponBV *= 0.8;
            calculation += " x 0.8 (Drone)";
        }

        // The weapon can be a Vibroblade (MiscType)
        if (weapon.getType() instanceof WeaponType) {
            WeaponType weaponType = (WeaponType) weapon.getType();

            // PPC with Capacitor
            if ((weapon.getLinkedBy() != null) && (weaponType.hasFlag(WeaponType.F_PPC))) {
                double capBV = ((MiscType) weapon.getLinkedBy().getType()).getBV(entity, weapon);
                weaponBV += capBV;
                calculation += " + " + formatForReport(capBV) + " (Cap)";
            }

            // artemis bumps up the value
            if ((weapon.getLinkedBy() != null) && (weapon.getLinkedBy().getType() instanceof MiscType)) {
                Mounted linkedBy = weapon.getLinkedBy();
                if (linkedBy.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    weaponBV *= 1.2;
                    calculation += " x 1.2 (Art-IV)";
                } else if (linkedBy.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                    weaponBV *= 1.1;
                    calculation += " x 1.1 (P-Art)";
                } else if (linkedBy.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    weaponBV *= 1.3;
                    calculation += " x 1.3 (Art-V)";
                } else if (linkedBy.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)
                        || linkedBy.getType().hasFlag(MiscType.F_APOLLO)) {
                    weaponBV *= 1.15;
                    calculation += " x 1.15 (" + (linkedBy.getType().hasFlag(MiscType.F_APOLLO) ? "Apollo)" : "RISC LPM)");
                }
            }

            if (weaponType.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTC) {
                weaponBV *= 1.25;
                calculation += " x 1.25 (TC)";
            } else if ((fireControlModifier() != 1) && !weaponType.hasFlag(WeaponType.F_INFANTRY)) {
                weaponBV *= fireControlModifier();
                calculation += " x " + fireControlModifier() + " (Fire Control)";
            }
        }

        if (usesWeaponHeat()) {
            if (heatEfficiencyExceeded) {
                weaponBV /= 2;
                calculation += " x 0.5 (Overheat)";
            } else {
                calculation += " (Heat: " + formatForReport(heatSum) + ")";
            }
        }

        String result = "";
        if (addToOffensiveValue) {
            offensiveValue += weaponBV;
            result = "= " + formatForReport(offensiveValue);
        }

        if (showInReport) {
            bvReport.addLine("- " + multiplierText + equipmentDescriptor(weapon), calculation, result);
        }
        return weaponBV;
    }

    /**
     * Returns a Predicate that determines if a weapon is counted as a front or nose arc weapon -ONLY-
     * for the purpose of determining if the weapon BV of front weapons exceeds that of
     * rear weapons on unit types that care about rear weapons.
     * By default, returns true so that all weapons count safely as front weapons. Should be
     * overridden for all unit types that account for rear-facing weapons. Does not need to
     * be overridden for unit types that don't (such as ProtoMeks).
     *
     * @return A Predicate identifying if a weapon counts as a front weapon (read above!)
     */
    protected Predicate<Mounted> frontWeaponFilter() {
        return weapon -> true;
    }

    /**
     * Returns a Predicate that determines if a weapon is counted as a rear or aft arc weapon -ONLY-
     * for the purpose of determining if the weapon BV of front weapons exceeds that of
     * rear weapons on unit types that care about rear weapons.
     * By default, returns false so that all weapons count safely as front weapons. Should be
     * overridden for all unit types that account for rear-facing weapons. Does not need to
     * be overridden for unit types that don't (such as ProtoMeks).
     *
     * @return A Predicate identifying if a weapon counts as a front weapon (read above!)
     */
    protected Predicate<Mounted> rearWeaponFilter() {
        return weapon -> false;
    }

    protected boolean countAsOffensiveWeapon(Mounted equipment) {
        if (equipment.getType() instanceof AmmoType) {
            return false;
        } else if (equipment.getType() instanceof MiscType) {
            return countMiscAsOffensiveWeapon(equipment);
        } else {
            WeaponType weaponType = (WeaponType) equipment.getType();
            return !weaponType.hasFlag(WeaponType.F_AMS) && !weaponType.hasFlag(WeaponType.F_B_POD)
                    && !weaponType.hasFlag(WeaponType.F_M_POD)
                    && ((weaponType.getBV(entity) > 0) || weaponType.hasFlag(WeaponType.F_MGA))
                    && !equipment.isInoperable() && !equipment.isHit()
                    && !equipment.isWeaponGroup() && !(weaponType.getAtClass() == WeaponType.CLASS_SCREEN)
                    && !(weaponType instanceof BayWeapon);
        }
    }

    protected boolean countMiscAsOffensiveWeapon(Mounted misc) {
        MiscType miscType = (MiscType) misc.getType();
        return (miscType.getBV(entity) > 0)
                && !misc.isHit() && !misc.isInoperable()
                && !misc.isWeaponGroup()
                && (miscType.isVibroblade() || miscType.hasFlag(MiscType.F_VIBROCLAW)
                || miscType.hasFlag(MiscType.F_MAGNET_CLAW));
    }

    /** @return The BV modifier for AFC or BFC. Override as necessary. */
    protected double fireControlModifier() {
        if (entity.isSupportVehicle()) {
            if (entity.hasWorkingMisc(MiscType.F_BASIC_FIRECONTROL)) {
                return 0.9;
            } else if (!entity.hasWorkingMisc(MiscType.F_ADVANCED_FIRECONTROL)) {
                return 0.8;
            }
        }
        return 1;
    }

    protected void processOffensiveEquipment() {
        bvReport.startTentativeSection();
        bvReport.addLine("Offensive Equipment:", "", "");
        boolean hasOffensiveEquipment = false;
        for (Mounted misc : entity.getMisc()) {
            MiscType mtype = (MiscType) misc.getType();

            // don't count destroyed equipment
            if (misc.isDestroyed()) {
                continue;
            }

            // Vibroblades are treated with weapons
            if ((misc.getType() instanceof MiscType)
                    && misc.getType().hasFlag(MiscType.F_CLUB)
                    && ((MiscType) misc.getType()).isVibroblade()) {
                continue;
            }

            if ((mtype.hasFlag(MiscType.F_ECM) && !mtype.hasFlag(MiscType.F_WATCHDOG))
                    || mtype.hasFlag(MiscType.F_AP_POD)
                    || mtype.hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)
                    || mtype.hasFlag(MiscType.F_VIRAL_JAMMER_HOMING)
                    || mtype.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                    || mtype.hasFlag(MiscType.F_CHAFF_POD)
                    || mtype.hasFlag(MiscType.F_BULLDOZER)
                    || mtype.hasFlag(MiscType.F_BAP)
                    || mtype.hasFlag(MiscType.F_TARGCOMP)
                    || mtype.hasFlag(MiscType.F_SPIKES)
                    || mtype.hasFlag(MiscType.F_MINESWEEPER)
                    || mtype.hasFlag(MiscType.F_HARJEL_II)
                    || mtype.hasFlag(MiscType.F_HARJEL_III)
                    || mtype.hasFlag(MiscType.F_MASS)
                    || mtype.hasFlag(MiscType.F_MINE)
                    || mtype.isShield()
                    || offensiveEquipmentBV(mtype, misc.getLocation()) == 0) {
                continue;
            }
            double bv = offensiveEquipmentBV(mtype, misc.getLocation());
            if ((mtype.hasFlag(MiscType.F_CLUB) || mtype.hasFlag(MiscType.F_HAND_WEAPON))
                    && entity.hasFunctionalArmAES(misc.getLocation())) {
                bv *= 1.25;
            } else if (mtype.hasFlag(MiscType.F_WATCHDOG)) {
                bv = 7;
            }
            offensiveValue += bv;
            bvReport.addLine("- " + equipmentDescriptor(misc), "+ " + formatForReport(bv),
                    "= " + formatForReport(offensiveValue));
            hasOffensiveEquipment = true;
        }
        bvReport.finalizeTentativeSection(hasOffensiveEquipment);
    }

    protected double offensiveEquipmentBV(MiscType misc, int location) {
        return misc.getBV(entity, location);
    }

    protected void processAmmo() {
        bvReport.startTentativeSection();
        bvReport.addLine("Ammo:", "", "");
        boolean hasAmmo = false;
        for (String key : keys) {
            if (!weaponsForExcessiveAmmo.containsKey(key)) {
                // Coolant Pods have no matching weapon
                if (key.equals(Integer.valueOf(AmmoType.T_COOLANT_POD).toString() + "1")) {
                    offensiveValue += ammoMap.get(key);
                }
                continue;
            }
            if (!ammoMap.containsKey(key)) {
                continue;
            }
            String calculation = "+ ";

            if (ammoMap.get(key) > weaponsForExcessiveAmmo.get(key)) {
                offensiveValue += weaponsForExcessiveAmmo.get(key) * fireControlModifier();
                calculation += formatForReport(weaponsForExcessiveAmmo.get(key)) + " (Excessive)";
            } else {
                offensiveValue += ammoMap.get(key) * fireControlModifier();
                calculation += formatForReport(ammoMap.get(key));
            }
            calculation += (fireControlModifier() != 1) ? " x " + formatForReport(fireControlModifier()) : "";
            bvReport.addLine("- " + names.get(key), calculation, "= " + formatForReport(offensiveValue));
            hasAmmo = true;
        }
        if (hasAmmo) {
            bvReport.endTentativeSection();
        } else {
            bvReport.discardTentativeSection();
        }
    }

    /** @return The unit's heat dissipation for BV purposes. Override as necessary. */
    protected int heatEfficiency() {
        return NO_HEAT;
    }

    protected void processWeight() { }

    protected void processSpeedFactor() {
        double speedFactor = offensiveSpeedFactor(offensiveSpeedFactorMP());
        bvReport.addLine("Speed Factor:",
                formatForReport(offensiveValue) + " x " + speedFactor,
                "= " + formatForReport(offensiveValue * speedFactor));
        offensiveValue *= speedFactor;
    }

    /**
     * Returns the Speed Factor (TM p.316) for the given MP parameter
     * @param mp The MP value for the unit (base value: Walk + 1/2 Jump/UMU)
     * @return The Speed Factor as a two-digit-rounded double such as 1.76
     */
    protected double offensiveSpeedFactor(int mp) {
        return Math.round(Math.pow(1 + ((mp - 5) / 10.0), 1.2) * 100.0) / 100.0;
    }

    /** @return the MP value to use for the Offensive Speed Factor (TM p.316) for this unit. */
    protected int offensiveSpeedFactorMP() {
        return runMP + (int) (Math.round(Math.max(jumpMP, umuMP) / 2.0));
    }

    /** Processes unit type modifiers of TM, p.316. */
    protected void processOffensiveTypeModifier() { }

    /** @return true when the given ammo (must be AmmoType) counts towards offensive ammo BV calculation. */
    protected boolean ammoCounts(Mounted ammo) {
        AmmoType ammoType = (AmmoType) ammo.getType();
        return (ammo.getUsableShotsLeft() > 0)
                && (ammoType.getAmmoType() != AmmoType.T_AMS)
                && (ammoType.getAmmoType() != AmmoType.T_APDS)
                && (ammoType.getAmmoType() != AmmoType.T_SCREEN_LAUNCHER)
                && !ammo.isOneShotAmmo();
    }

    /** Processes the sum of offensive and defensive battle rating and modifiers that affect this sum. */
    protected void processSummarize() {
        double cockpitMod = 1;
        String modifier = "";
        if (isDrone) {
            cockpitMod = 0.95;
            modifier = " (Drone Op. Sys.)";
        }
        baseBV = defensiveValue + offensiveValue;
        bvReport.addEmptyLine();
        bvReport.addSubHeader("Battle Value:");
        if (cockpitMod != 1) {
            bvReport.addLine("Defensive BR + Offensive BR:",
                    formatForReport(defensiveValue) + " + " + formatForReport(offensiveValue),
                    "= " + formatForReport(baseBV));
            bvReport.addLine("Cockpit Modifier:",
                    formatForReport(baseBV) + " x " + formatForReport(cockpitMod) + modifier,
                    "= " + formatForReport(baseBV * cockpitMod));
            baseBV *= cockpitMod;
            bvReport.addLine("--- Base Unit BV:", "" + (int) Math.round(baseBV));
        } else {
            bvReport.addLine("--- Base Unit BV:",
                    formatForReport(defensiveValue) + " + " + formatForReport(offensiveValue) + ", rn",
                    "= " + (int) Math.round(baseBV));
        }
    }

    protected void assembleAmmo() {
        for (Mounted ammo : entity.getAmmo()) {
            AmmoType ammoType = (AmmoType) ammo.getType();

            // don't count depleted ammo, AMS and oneshot ammo
            if (ammoCounts(ammo)) {
                String key = ammoType.getAmmoType() + ":" + ammoType.getRackSize();
                if (!keys.contains(key)) {
                    keys.add(key);
                    names.put(key, equipmentDescriptor(ammo));
                }
                if (!ammoMap.containsKey(key)) {
                    ammoMap.put(key, getAmmoBV(ammo));
                } else {
                    ammoMap.put(key, getAmmoBV(ammo) + ammoMap.get(key));
                }
            }
        }

        for (Mounted weapon : entity.getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) weapon.getType();

            if (weapon.isDestroyed() //|| wtype.hasFlag(WeaponType.F_AMS)
                    || wtype.hasFlag(WeaponType.F_B_POD) || wtype.hasFlag(WeaponType.F_M_POD)
                    || wtype instanceof BayWeapon || weapon.isWeaponGroup()) {
                continue;
            }

            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !((wtype.getAmmoType() == AmmoType.T_PLASMA)
                    || (wtype.getAmmoType() == AmmoType.T_VEHICLE_FLAMER)
                    || (wtype.getAmmoType() == AmmoType.T_HEAVY_FLAMER)
                    || (wtype.getAmmoType() == AmmoType.T_CHEMICAL_LASER)))
                    || wtype.hasFlag(WeaponType.F_ONESHOT)
                    || wtype.hasFlag(WeaponType.F_INFANTRY)
                    || (wtype.getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(entity));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(entity) + weaponsForExcessiveAmmo.get(key));
                }
            }
        }
    }

    protected double getAmmoBV(Mounted ammo) {
        return ammo.getType().getBV(entity);
    }

    /**
     * Adjust the BV with force bonuses (TAG, C3) and pilot skill.
     */
    protected void adjustBV() {
        adjustedBV = (int) Math.round(baseBV);
        double c3Bonus = ignoreC3 ? 0 : entity.getExtraC3BV((int) Math.round(adjustedBV));
        List<String> pilotModifiers = new ArrayList<>();
        double pilotFactor = ignoreSkill ? 1 : BVCalculator.bvMultiplier(entity, pilotModifiers);

        processTagBonus();

        if (c3Bonus > 0) {
            adjustedBV += c3Bonus;
            bvReport.addLine("Force Bonus (C3):",
                    "+ " + formatForReport(c3Bonus), "= " + formatForReport(adjustedBV));
        }

        processExternalStores();

        if ((pilotFactor != 1) || !pilotModifiers.isEmpty()) {
            bvReport.addLine("Pilot Modifier:",
                    formatForReport(adjustedBV) + " x " + formatForReport(pilotFactor)
                            + (pilotModifiers.isEmpty() ? "" : " (" + String.join(", ", pilotModifiers) + ")"),
                    "= " + formatForReport(adjustedBV * pilotFactor));
            adjustedBV *= pilotFactor;
        }

        if ((adjustedBV != (int) Math.round(baseBV)) || !pilotModifiers.isEmpty()) {
            bvReport.addLine("--- Adjusted BV:", formatForReport(adjustedBV) + ", rn",
                    "= " + (int) Math.round(adjustedBV));
        }
    }

    private static final double[][] bvMultipliers = new double[][] {
            {2.42, 2.31, 2.21, 2.10, 1.93, 1.75, 1.68, 1.59, 1.50},
            {2.21, 2.11, 2.02, 1.92, 1.76, 1.60, 1.54, 1.46, 1.38},
            {1.93, 1.85, 1.76, 1.68, 1.54, 1.40, 1.35, 1.28, 1.21},
            {1.66, 1.58, 1.51, 1.44, 1.32, 1.20, 1.16, 1.10, 1.04},
            {1.38, 1.32, 1.26, 1.20, 1.10, 1.00, 0.95, 0.90, 0.85},
            {1.31, 1.19, 1.13, 1.08, 0.99, 0.90, 0.86, 0.81, 0.77},
            {1.24, 1.12, 1.07, 1.02, 0.94, 0.85, 0.81, 0.77, 0.72},
            {1.17, 1.06, 1.01, 0.96, 0.88, 0.80, 0.76, 0.72, 0.68},
            {1.10, 0.99, 0.95, 0.90, 0.83, 0.75, 0.71, 0.68, 0.64},
    };

    /**
     * Returns the BV multiplier for the gunnery/piloting of the given entity's pilot (TM p.315) as well as MD
     * implants of the pilot.
     * Returns 1 if the given entity's crew is null. Special treatment is given to infantry units where
     * units unable to make anti-mek attacks use 5 as their anti-mek (piloting) value as well as LAM pilots that
     * use the average of their aero and mek values.
     *
     * @param entity The entity to get the skill modifier for
     * @return The BV multiplier for the given entity's pilot
     */
    public static double bvMultiplier(Entity entity, List<String> pilotModifiers) {
        if (entity.getCrew() == null) {
            if (entity.isConventionalInfantry() && !((Infantry) entity).hasAntiMekGear()) {
                return bvSkillMultiplier(4, Infantry.ANTI_MECH_SKILL_NO_GEAR);
            } else {
                return bvSkillMultiplier(4, 5);
            }
        }
        int gunnery = entity.getCrew().getGunnery();
        int piloting = entity.getCrew().getPiloting();

        if (((entity instanceof Infantry) && (!((Infantry) entity).canMakeAntiMekAttacks()))
                || (entity instanceof Protomech)) {
            piloting = 5;
        } else if (entity.isConventionalInfantry() && !((Infantry) entity).hasAntiMekGear()) {
            piloting = Infantry.ANTI_MECH_SKILL_NO_GEAR;
        } else if (entity.getCrew() instanceof LAMPilot) {
            LAMPilot lamPilot = (LAMPilot) entity.getCrew();
            gunnery = (lamPilot.getGunneryMech() + lamPilot.getGunneryAero()) / 2;
            piloting = (lamPilot.getPilotingMech() + lamPilot.getPilotingAero()) / 2;
        }
        double skillMultiplier = bvSkillMultiplier(gunnery, piloting);
        if (skillMultiplier != 1) {
            pilotModifiers.add("Skill");
        }

        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_PAIN_SHUNT)) {
            piloting = Math.max(0, piloting - 1);
            pilotModifiers.add("Pain Shunt");
        }
        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_COMM_IMPLANT)) {
            piloting = Math.max(0, piloting - 1);
            pilotModifiers.add("Comm. Implant");
        }
        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_VDNI)
                && entity.hasMisc(MiscType.F_BATTLEMECH_NIU)) {
            piloting = Math.max(0, piloting - 1);
            gunnery = Math.max(0, gunnery - 1);
            pilotModifiers.add("VDNI");
        }
        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_BVDNI)
                && entity.hasMisc(MiscType.F_BATTLEMECH_NIU)) {
            gunnery = Math.max(0, gunnery - 1);
            pilotModifiers.add("Buf. VDNI");
        }
        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_MM_IMPLANTS)
                || entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_ENH_MM_IMPLANTS)
                || entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_CYBER_IMP_LASER)
                || entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_CYBER_IMP_AUDIO)
                || entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_CYBER_IMP_VISUAL)) {
            gunnery = Math.max(0, gunnery - 1);
            pilotModifiers.add("Sensory Implants");
        }
        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_PROTO_DNI)
                && entity.hasMisc(MiscType.F_BATTLEMECH_NIU)) {
            piloting = Math.max(0, piloting - 3);
            gunnery = Math.max(0, gunnery - 2);
            pilotModifiers.add("Proto DNI");
        }
        if (entity.getCrew().getOptions().booleanOption(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR)) {
            if (entity.isMek() || entity.isCombatVehicle() || entity.isAerospaceFighter()) {
                gunnery = Math.max(0, gunnery - 1);
            } else {
                piloting = Math.max(0, piloting - 1);
            }
            pilotModifiers.add("TCP");
        }
        return bvSkillMultiplier(gunnery, piloting);
    }

    /**
     * Returns the BV multiplier for the given gunnery and piloting values. Returns 1 for the neutral
     * values 4/5.
     *
     * @param gunnery  the gunnery skill of a pilot
     * @param piloting the piloting skill of a pilot
     * @return a multiplier to the BV of whatever unit the pilot is piloting.
     */
    public static double bvSkillMultiplier(int gunnery, int piloting) {
        return bvMultipliers[MathUtility.clamp(gunnery, 0, 8)][MathUtility.clamp(piloting, 0, 8)];
    }

    /**
     * Processes the BV bonus that a unit with TAG, LTAG or C3M gets for friendly units that have semi-guided
     * or Arrow IV homing ammunition
     * (TO:AUE p.198, https://bg.battletech.com/forums/tactical-operations/tagguided-munitions-and-bv/)
     */
    public void processTagBonus() {
        long tagCount = workingTAGCount(entity);
        if ((tagCount == 0) || (entity.getGame() == null)) {
            return;
        }

        bvReport.startTentativeSection();
        bvReport.addLine("TAG Bonus (" + tagCount + " Tag" + (tagCount == 1 ? "" : "s") + "):", "", "");
        // In the lobby, bombs are represented as a bombChoices array, only later is it real Mounteds.
        boolean hasGuided = false;

        for (Entity otherEntity : entity.getGame().getEntitiesVector()) {
            if ((otherEntity == entity) || otherEntity.getOwner().isEnemyOf(entity.getOwner())) {
                continue;
            }
            for (Mounted mounted : otherEntity.getAmmo()) {
                AmmoType atype = (AmmoType) mounted.getType();
                EnumSet<AmmoType.Munitions> munitionType = atype.getMunitionType();
                if ((mounted.getUsableShotsLeft() > 0)
                        && ((munitionType.contains(AmmoType.Munitions.M_SEMIGUIDED))
                            || (munitionType.contains(AmmoType.Munitions.M_HOMING)))) {
                    adjustedBV += mounted.getType().getBV(entity) * tagCount;
                    bvReport.addLine("- " + equipmentDescriptor(mounted),
                            "+ " + tagCount + " x " + formatForReport(mounted.getType().getBV(entity))
                                    + " (" + otherEntity.getShortName() + ")",
                            "= " + formatForReport(adjustedBV));
                    hasGuided = true;
                }
            }
            if (otherEntity instanceof IBomber) {
                IBomber asBomber = (IBomber) otherEntity;
                BombType bomb = BombType.createBombByType(BombType.B_HOMING);
                int homingCount = asBomber.getBombChoices()[BombType.B_HOMING];
                if (homingCount > 0) {
                    adjustedBV += bomb.getBV(otherEntity) * asBomber.getBombChoices()[BombType.B_HOMING] * tagCount;
                    bvReport.addLine("- " + bomb.getName(),
                            "+ " + tagCount + " x " + formatForReport(bomb.getBV(otherEntity))
                                    + " (" + otherEntity.getShortName() + ")",
                            "= " + formatForReport(adjustedBV));
                    hasGuided = true;
                }
            }
        }
        bvReport.finalizeTentativeSection(hasGuided);
    }

    protected long workingTAGCount(Entity entity) {
        long tagCount = entity.getWeaponList().stream()
                .filter(m -> !m.isMissing() && !m.isDestroyed())
                .map(Mounted::getType)
                .filter(Objects::nonNull)
                .filter(t -> t.hasFlag(WeaponType.F_TAG))
                .count();
        if (entity instanceof IBomber) {
            IBomber asBomber = (IBomber) entity;
            tagCount += asBomber.getBombChoices()[BombType.B_TAG];
        }
        return tagCount;
    }

    protected void processExternalStores() {
        bvReport.startTentativeSection();
        bvReport.addLine("External Stores:", "", "");
        // In the lobby, bombs are represented as a bombChoices array, only later is it real Mounteds.
        boolean hasBombs = false;
        if (entity instanceof IBomber) {
            IBomber asBomber = (IBomber) entity;
            for (BombType bombType : BombType.allBombTypes()) {
                int bombCount = asBomber.getBombChoices()[BombType.getBombTypeFromInternalName(bombType.getInternalName())];
                double bombTypeBV = bombType.getBV(entity);
                if ((bombCount > 0) && (bombTypeBV > 0)) {
                    double bombBV = bombType.getBV(entity) * bombCount;
                    adjustedBV += bombBV;
                    bvReport.addLine("- " + bombType.getName(),
                            "+ " + bombCount + " x " + formatForReport(bombType.getBV(entity)),
                            "= " + formatForReport(adjustedBV));
                    hasBombs = true;
                }
            }
            for (Mounted bomb : entity.getBombs()) {
                double bombBV = processWeapon(bomb, true, false);
                if (bombBV > 0) {
                    adjustedBV += bombBV;
                    hasBombs = true;
                }

            }
        }
        bvReport.finalizeTentativeSection(hasBombs);
    }

    private double armorMultiplier(int location) {
        double armorMultiplier;
        switch (entity.getArmorType(location)) {
            case EquipmentType.T_ARMOR_HARDENED:
                armorMultiplier = 2.0;
                break;
            case EquipmentType.T_ARMOR_REACTIVE:
            case EquipmentType.T_ARMOR_REFLECTIVE:
            case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                armorMultiplier = 1.5;
                break;
            case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
            case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                armorMultiplier = 1.2;
                break;
            case EquipmentType.T_ARMOR_HEAT_DISSIPATING:
                armorMultiplier = 1.1;
                break;
            default:
                armorMultiplier = 1.0;
                break;
        }

        if (hasBlueShield) {
            armorMultiplier += 0.2;
        }
        if (entity.countWorkingMisc(MiscType.F_HARJEL_II, location) > 0) {
            armorMultiplier *= 1.1;
        }
        if (entity.countWorkingMisc(MiscType.F_HARJEL_III, location) > 0) {
            armorMultiplier *= 1.2;
        }
        return armorMultiplier;
    }

    private String armorMultiplierText(int location) {
        List<String> modifiers = new ArrayList<>();
        switch (entity.getArmorType(location)) {
            case EquipmentType.T_ARMOR_COMMERCIAL:
            case EquipmentType.T_ARMOR_HARDENED:
            case EquipmentType.T_ARMOR_REACTIVE:
            case EquipmentType.T_ARMOR_REFLECTIVE:
            case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
            case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
            case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
            case EquipmentType.T_ARMOR_HEAT_DISSIPATING:
                modifiers.add(EquipmentType.getArmorTypeName(entity.getArmorType(location)));
        }

        if (hasBlueShield) {
            modifiers.add("Blue Shield");
        }
        if (entity.countWorkingMisc(MiscType.F_HARJEL_II, location) > 0) {
            modifiers.add("HarJel II");
        }
        if (entity.countWorkingMisc(MiscType.F_HARJEL_III, location) > 0) {
            modifiers.add("HarJel III");
        }
        return modifiers.isEmpty() ? "" : " (" + String.join(", ", modifiers) + ")";
    }
}