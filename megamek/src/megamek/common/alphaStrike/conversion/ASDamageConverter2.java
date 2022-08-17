/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.alphaStrike.conversion;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.alphaStrike.*;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.common.weapons.other.CLFussilade;

import java.util.*;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.ITechnology.TECH_BASE_CLAN;
import static megamek.common.alphaStrike.AlphaStrikeElement.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASDamageConverter2 {

    // Used for CI and BA, ASC p.102
    protected static final int[] TROOP_FACTOR = {
            0, 0, 1, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12,
            13, 14, 15, 16, 16, 17, 17, 17, 18, 18
    };

    private static final List<Double> ZERO_DAMAGE = List.of(0d, 0d, 0d, 0d);

    protected Entity entity;
    protected AlphaStrikeElement element;
    protected CalculationReport report;

    //    private ASConverter.WeaponLocation[] weaponLocations;
    protected String[] locationNames;
    private int[] heat;
    // The locations where damage and special abilities are going to end up in. [0] contains the unit's standard
    // damage and central abilities, the others can be turrets, arcs or rear.
    protected ASArcSummary[] locations;
    private final Map<WeaponType, Boolean> ammoForWeapon = new HashMap<>();
    protected final boolean hasTargetingComputer;
    protected ArrayList<Mounted> weaponsList;
    private boolean needsHeatAdjustment = false;
    private double heatAdjustFactorSM = 1;
    protected double heatAdjustFactorLE = 1;

    protected ASDamage finalSDamage;
    protected ASDamage finalMDamage;
    protected ASDamage finalLDamage;
    protected ASDamage finalEDamage;

    static ASDamageConverter2 getASDamageConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        if (element.isBattleArmor()) {
            return new ASBattleArmorDamageConverter(entity, element, report);
        } else if (element.isConventionalInfantry()) {
            return new ASConvInfantryDamageConverter(entity, element, report);
        } else if (element.usesArcs()) {
            return new ASArcedDamageConverter(entity, element, report);
        } else if (element.isAerospace()) {
            return new ASAeroDamageConverter(entity, element, report);
        } else {
            return new ASDamageConverter2(entity, element, report);
        }
    }

    /**
     * Do not call this directly. Use the static getASDamageConverter instead.
     * Constructs a damage converter for ground units.
     *
     * @param entity The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report The calculation report to write to
     */
    protected ASDamageConverter2(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        this.element = element;
        this.entity = entity;
        this.report = report;
        locations = new ASArcSummary[ASLocationMapper.damageLocationsCount(entity)];
        locationNames = new String[locations.length];
        for (int index = 0; index < locationNames.length; index++) {
            locationNames[index] = ASLocationMapper.locationName(entity, index);
            locations[index] = ASArcSummary.createTurretSummary(element);
        }
        hasTargetingComputer = entity.hasTargComp();
    }

    void convertDamage2() {
        report.addEmptyLine();
        report.addSubHeader("Damage Conversion:");

//        double[] baseDamage = new double[element.getRangeBands()];
        boolean hasTC = entity.hasTargComp();
        int[] ranges;
        double pointDefense = 0;
        int bombRacks = 0;
        double baseIFDamage = 0;
        // Track weapons we've already calculated ammunition for
//        HashMap<String, Boolean> ammoForWeapon = new HashMap<>();
        assembleAmmoCounts();
//        boolean needsHeatAdjustment = false;
//        int heatCapacity = getHeatCapacity();
//        int mediumRangeFrontHeat = getHeatGeneration(false, false);

        weaponsList = new ArrayList<>(entity.getWeaponList());
        weaponsList.removeIf(Objects::isNull);

        processSpecialAbilities();
        processDamage();
        finalizeSpecialAbilities();
    }

    protected void finalizeSpecialAbilities() { }

    protected void processSpecialAbilities() {
        report.addEmptyLine();
        report.addLine("--- Special Abilities:", "");
        for (Mounted weapon : weaponsList) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            assignSpecialAbilities(weapon, weaponType);
        }
        if (foundNoSpecialAbility()) {
            report.addLine("None", "", "");
        }
    }

    private boolean foundNoSpecialAbility() {
        return Arrays.stream(locations).map(ASArcSummary::getSpecials).allMatch(ASSpecialAbilityCollection::isEmpty);
    }

    protected void processDamage() {
        calculateHeatAdjustment();
        processSDamage();
        processMDamage();
        processLDamage();
    }

    protected void calculateHeatAdjustment() {
        if (element.usesOV()) {
            int heatCapacity = getHeatCapacity();
            int mediumRangeFrontHeat = getHeatGeneration(false, false);
            if (mediumRangeFrontHeat - 4 > heatCapacity) {
                needsHeatAdjustment = true;
                heatAdjustFactorSM = (double) heatCapacity / (mediumRangeFrontHeat - 4);
            }
        }
    }

    /**
     * Sums up the front-facing damage values for the given range and reports the counted weapons.
     * Includes modifiers for low ammo, AES etc. and the location multiplier (0.5 for some Aero arcs)
     *
     * @param weaponsList The list of (non-null) weapons of the entity
     * @param range The range, e.g. AlphaStrikeElement.MEDIUM_RANGE
     * @return The raw damage sum
     */
    protected double calculateFrontDamage(List<Mounted> weaponsList, int range) {
        double rawDamage = 0;
        for (Mounted weapon : weaponsList) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            //TODO: replace the range with the range bracket; ranges in hexes have no place in AS conversion:
            double baseDamage = determineDamage(weapon, range);
            double locationMultiplier = ASLocationMapper.damageLocationMultiplier(entity, 0, weapon);
            if ((locationMultiplier > 0) && (baseDamage > 0)) {
                double damageModifier = getDamageMultiplier(weapon, weaponType);
                double modifiedDamage = baseDamage * damageModifier * locationMultiplier;
                String calculation = (damageModifier != 1) ? baseDamage + " x " + damageModifier : "";
                // TODO: put the value in the calculation and the sum in the result
                calculation += (locationMultiplier != 1) ? " x " + formatForReport(locationMultiplier) : "";
                report.addLine(weapon.getName(), calculation, "+ " + formatForReport(modifiedDamage));
                rawDamage += modifiedDamage;
            }
        }
        return rawDamage;
    }

    protected double determineDamage(Mounted weapon, int range) {
        return ((WeaponType) weapon.getType()).getBattleForceDamage(range);
    }

    protected void processSDamage() {
        report.addEmptyLine();
        report.addLine("--- Short Range Damage:", "");
        double sDamage = calculateFrontDamage(weaponsList, SHORT_RANGE);

        if (needsHeatAdjustment) {
            report.addLine("Adjusted Damage: ",
                    formatForReport(sDamage) + " x (see M)",
                    "= " + formatForReport(sDamage * heatAdjustFactorSM));
            sDamage = sDamage * heatAdjustFactorSM;
        }

        finalSDamage = ASDamage.createDualRoundedUp(sDamage);
        report.addLine("Final S damage:",
                formatForReport(sDamage) + ", dual rounded", "= " + finalSDamage.toStringWithZero());
    }

    protected void processMDamage() {
        report.addEmptyLine();
        report.addLine("--- Medium Range Damage:", "");
        double rawMDamage = calculateFrontDamage(weaponsList, MEDIUM_RANGE);
        double mDamage = rawMDamage;
        int roundedUpRaw = ASConverter.roundUp(roundUpToTenth(rawMDamage));
        int roundedUpAdjusted = 0;

        if (element.usesOV()) {
            int heatCapacity = getHeatCapacity();
            int mediumRangeFrontHeat = getHeatGeneration(false, false);
            report.addLine("Heat Generation M: ", formatForReport(mediumRangeFrontHeat), "");
            if (!needsHeatAdjustment) {
                report.addLine("Heat Capacity: ", heatCapacity + ", no heat adjustment", "");
            } else {
                report.addLine("Heat Capacity: ", heatCapacity + "", "");
                report.addLine("Raw M damage:",
                        formatForReport(rawMDamage) + ", dual rounded", "= " + roundedUpRaw);
                double mDamageAdjusted = rawMDamage * heatAdjustFactorSM;
                roundedUpAdjusted = ASConverter.roundUp(roundUpToTenth(mDamageAdjusted));
                element.setOverheat(Math.min(roundedUpRaw - roundedUpAdjusted, 4));
                report.addLine("Adjusted Damage: ",
                        formatForReport(rawMDamage) + " x " + heatCapacity + " / (" + mediumRangeFrontHeat + " - 4)",
                        "= " + formatForReport(mDamageAdjusted));
                mDamage = mDamageAdjusted;
            }
        }

        finalMDamage = ASDamage.createDualRoundedUp(mDamage);
        report.addLine("Final M damage:",
                formatForReport(mDamage) + ", dual rounded", "= " + finalMDamage.toStringWithZero());

        if (element.hasOV()) {
            report.addLine("Damage difference",
                    roundedUpRaw + " - " + roundedUpAdjusted,
                    "OV " + element.getOverheat());
        }
    }

    protected void processLDamage() {
        report.addEmptyLine();
        report.addLine("--- Long Range Damage:", "");
        double rawLDamage = calculateFrontDamage(weaponsList, LONG_RANGE);
        double lDamage = rawLDamage;
        int roundedUpRaw = ASConverter.roundUp(roundUpToTenth(rawLDamage));
        int roundedUpAdjusted = 0;

        // Determine OVL from long range damage and heat
        if (element.getOverheat() > 0) {
            int heatCapacity = getHeatCapacity();
            double longRangeFrontHeat = getHeatGeneration(false, true);
            report.addLine("Heat Generation L: ", formatForReport(longRangeFrontHeat), "");
            if (longRangeFrontHeat - 4 <= heatCapacity) {
                report.addLine("Heat Capacity: ", heatCapacity + ", no heat adjustment", "");
            } else {
                report.addLine("Heat Capacity: ", heatCapacity + "", "");
                heatAdjustFactorLE = (double) heatCapacity / (longRangeFrontHeat - 4);
                double lDamageAdjusted = rawLDamage * heatAdjustFactorLE;
                roundedUpAdjusted = ASConverter.roundUp(roundUpToTenth(lDamageAdjusted));
                report.addLine("Raw L damage:",
                        formatForReport(rawLDamage) + ", dual rounded", "= " + roundedUpRaw);
                report.addLine("Adjusted Damage: ",
                        formatForReport(rawLDamage) + " x " + heatCapacity + " / ("
                                + formatForReport(longRangeFrontHeat) + " - 4)",
                        "= " + formatForReport(lDamageAdjusted));
                if (roundedUpAdjusted < roundedUpRaw) {
                    element.addSPA(OVL);
                    lDamage = lDamageAdjusted;
                } else {
                    report.addLine("Damage difference",
                            roundedUpRaw + " = " + roundedUpAdjusted + ", no OVL, no adjustment", "");
                }
            }
        }

        finalLDamage = ASDamage.createDualRoundedUp(lDamage);
        report.addLine("Final L damage:",
                formatForReport(lDamage) + ", dual rounded", "= " + finalLDamage.toStringWithZero());

        if (element.hasSUA(OVL)) {
            report.addLine("Damage difference",
                    roundedUpRaw + " > " + roundedUpAdjusted,
                    "OVL");
        }
    }

    protected void processEDamage() { }

    protected double getDamageMultiplier(Mounted weapon, WeaponType weaponType) {
        // Low ammo count
        double damageModifier = ammoForWeapon.getOrDefault(weaponType, true) ? 1 : 0.75;

        // Oneshot or Fusillade
        if (weaponType.hasFlag(WeaponType.F_ONESHOT) && !(weaponType instanceof CLFussilade)) {
            damageModifier *= .1;
        }

        // Targetting Computer
        if (hasTargetingComputer && weaponType.hasFlag(WeaponType.F_DIRECT_FIRE)
                && (weaponType.getAmmoType() != AmmoType.T_AC_LBX)
                && (weaponType.getAmmoType() != AmmoType.T_AC_LBX_THB)) {
            damageModifier *= 1.10;
        }

        // Actuator Enhancement System
        if (entity.hasWorkingMisc(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM, -1, weapon.getLocation())
                && ((weapon.getLocation() == Mech.LOC_LARM) || (weapon.getLocation() == Mech.LOC_RARM))) {
            damageModifier *= 1.05;
        }

        return damageModifier;
    }

    private void assembleAmmoCounts() {
        ArrayList<Mounted> weaponsList = new ArrayList<>(entity.getWeaponList());
        weaponsList.removeIf(Objects::isNull);
        Map<WeaponType, Integer> weaponCount = new HashMap<>();
        // Get weapon counts
        for (Mounted weapon : weaponsList) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            if ((weaponType.getAmmoType() != AmmoType.T_NA)
                    && !weaponType.hasFlag(WeaponType.F_ONESHOT)
                    && (!(entity instanceof BattleArmor) || weaponType instanceof MissileWeapon)) {
                weaponCount.merge(weaponType, 1, Integer::sum);
            }
        }
        // Get ammo counts per weapon type
        for (WeaponType weaponType : weaponCount.keySet()) {
            int ammoCount = 0;
            for (Mounted ammo : entity.getAmmo()) {
                AmmoType ammoType = (AmmoType) ammo.getType();
                if ((ammoType.getAmmoType() == weaponType.getAmmoType())
                        && (ammoType.getRackSize() == weaponType.getRackSize())) {
                    int divisor = 1;
                    if (ammoType.getAmmoType() == AmmoType.T_AC_ROTARY) {
                        divisor = 6;
                    } else if (ammoType.getAmmoType() == AmmoType.T_AC_ULTRA
                            || ammoType.getAmmoType() == AmmoType.T_AC_ULTRA_THB) {
                        divisor = 2;
                    }
                    ammoCount += ammo.getUsableShotsLeft() / divisor;
                }

            }
            ammoForWeapon.put(weaponType, ammoCount >= 10);
        }
    }

    protected void assignSpecialAbilities(Mounted weapon, WeaponType weaponType) {
        if (weaponType.getAtClass() == WeaponType.CLASS_SCREEN) {
            assignToLocations(weapon, SCR, 1);
        }

        if (weaponType.hasFlag(WeaponType.F_TAG)) {
            if (weaponType.hasFlag(WeaponType.F_C3MBS)) {
                assignToLocations(weapon, C3BSM, 1);
                assignToLocations(weapon, MHQ, 6);
            } else if (weaponType.hasFlag(WeaponType.F_C3M)) {
                assignToLocations(weapon, C3M, 1);
                assignToLocations(weapon, MHQ, 5);
            }
            assignToLocations(weapon, (weaponType.getShortRange() < 5) ? LTAG : TAG);
        }

        if (weaponType.hasFlag(WeaponType.F_TSEMP)) {
            assignToLocations(weapon, weaponType.hasFlag(WeaponType.F_ONESHOT) ? TSEMPO : TSEMP, 1);
        }

        processNarc(weapon, weaponType);
        processTaser(weapon, weaponType);
        processAMS(weapon, weaponType);
        processArtillery(weapon, weaponType);
    }

    protected void processNarc(Mounted weapon, WeaponType weaponType) {
        if (weaponType.getAmmoType() == AmmoType.T_INARC) {
            assignToLocations(weapon, INARC, 1);
        } else if (weaponType.getAmmoType() == AmmoType.T_NARC) {
            assignToLocations(weapon, SNARC, 1);
        }
    }

    protected void processTaser(Mounted weapon, WeaponType weaponType) {
        if (weaponType.getAmmoType() == AmmoType.T_TASER) {
            assignToLocations(weapon, MTAS, 1);
        }
    }

    protected void processAMS(Mounted weapon, WeaponType weaponType) {
        if (weaponType.hasFlag(WeaponType.F_AMS)) {
            assignToLocations(weapon, AMS);
        }
    }

    protected void processArtillery(Mounted weapon, WeaponType weaponType) {
        if (weaponType.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
            assignToLocations(weapon, getArtilleryType(weaponType), 1);
        }
    }

    /**
     * Checks the location multiplier for all locations and assigns the given SUA to any location where
     * the multiplier is not zero, i.e. to any location that the weapon is counted towards.
     *
     * @param weapon The weapon to check
     * @param sua The special unit ability to add
     */
    protected void assignToLocations(Mounted weapon, BattleForceSUA sua) {
        for (int loc = 0; loc < locations.length; loc++) {
            if ((ASLocationMapper.damageLocationMultiplier(entity, loc, weapon) != 0)
                    && !locations[loc].hasSPA(sua)) {
                locations[loc].getSpecials().addSPA(sua);
                reportAssignToLocations(weapon, sua, "", loc);
            }
        }
    }

    /**
     * Checks the location multiplier for all locations and assigns the given SUA with the given ability
     * value to any location where the multiplier is not zero, i.e. to any location that the weapon is
     * counted towards. If the SUA is already present, the ability value is added.
     *
     * @param weapon The weapon to check
     * @param sua The special unit ability to add
     * @param abilityValue The ability value to add
     */
    protected void assignToLocations(Mounted weapon, BattleForceSUA sua, int abilityValue) {
        for (int loc = 0; loc < locations.length; loc++) {
            if (ASLocationMapper.damageLocationMultiplier(entity, loc, weapon) != 0) {
                locations[loc].getSpecials().addSPA(sua, abilityValue);
                reportAssignToLocations(weapon, sua, abilityValue + "", loc);
            }
        }
    }

    /**
     * Checks the location multiplier for all locations and assigns the given SUA with the given ability
     * value to any location where the multiplier is not zero, i.e. to any location that the weapon is
     * counted towards. If the SUA is already present, the ability value is added.
     *
     * @param weapon The weapon to check
     * @param sua The special unit ability to add
     * @param abilityValue The ability value to add
     */
    protected void assignToLocations(Mounted weapon, BattleForceSUA sua, double abilityValue) {
        for (int loc = 0; loc < locations.length; loc++) {
            if (ASLocationMapper.damageLocationMultiplier(entity, loc, weapon) != 0) {
                locations[loc].getSpecials().addSPA(sua, abilityValue);
                reportAssignToLocations(weapon, sua, formatForReport(abilityValue), loc);
            }
        }
    }

    private void reportAssignToLocations(Mounted weapon, BattleForceSUA sua, String abilityValue, int loc) {
        String locationText = (loc > 0) ? " (" + ASLocationMapper.locationName(entity, loc) + ")" : "";
        String weaponText = weapon.getName() + " (" + entity.getLocationAbbr(weapon.getLocation()) + ")";
        report.addLine(weaponText, "+ " + sua.toString() + abilityValue + locationText);
    }

//            void convertDamage() {
//        report.addEmptyLine();
//        report.addSubHeader("Damage:");
//
//        double[] baseDamage = new double[element.getRangeBands()];
//        boolean hasTC = entity.hasTargComp();
//        int[] ranges;
//        double pointDefense = 0;
//        int bombRacks = 0;
//        double baseIFDamage = 0;
//        //TODO: multiple turrets
//        var turretsArcs = new HashMap<String, ASArcSummary>();
//        if (!element.usesArcs()) {
//            for (int loc = 0; loc < weaponLocations.length; loc++) {
//                if (locationNames[loc].startsWith("TUR")) {
//                    turretsArcs.put(locationNames[loc], ASArcSummary.createTurretSummary(element));
//                }
//            }
//        }
//        // Track weapons we've already calculated ammunition for
//        HashMap<String, Boolean> ammoForWeapon = new HashMap<>();
//
//        ArrayList<Mounted> weaponsList = entity.getWeaponList();
//
//        for (int pos = 0; pos < weaponsList.size(); pos++) {
//            Arrays.fill(baseDamage, 0);
//            double damageModifier = 1;
//            Mounted mount = weaponsList.get(pos);
//            if (mount == null) {
//                continue;
//            }
//
//            WeaponType weapon = (WeaponType) mount.getType();
//
//            //TODO: Remove this: The only thing that counts is the range bracket
//            ranges = STANDARD_RANGES;
//
//

//
//                for (int r = 0; r < element.getRangeBands(); r++) {
//                    if (entity instanceof BattleArmor) {
//                        baseDamage[r] = baseDamage[r] = getBattleArmorDamage(weapon, ranges[r], ((BattleArmor) entity),
//                                mount.isAPMMounted());
//                        baseIFDamage = baseDamage[RANGE_BAND_LONG];
//                    } else {
//                        baseDamage[r] = weapon.getBattleForceDamage(ranges[r], mount.getLinkedBy());
//                        // Disregard any Artemis bonus for IF:
//                        baseIFDamage = baseDamage[RANGE_BAND_LONG];
//                        if (MountedHelper.isAnyArtemis(mount.getLinkedBy()) && isSRMorLRMSpecial(weapon)) {
//                            baseIFDamage = weapon.getBattleForceDamage(ranges[r], null);
//                        }
//                    }
//                    for (int loc = 0; loc < weaponLocations.length; loc++) {
//                        Integer ht = weaponLocations[loc].heatDamage.get(r);
//                        if (ht == null) {
//                            ht = 0;
//                        }
//                        ht += (int) (ASLocationMapper.damageLocationMultiplier(entity, loc, mount) * weapon.getBattleForceHeatDamage(r));
//                        weaponLocations[loc].heatDamage.set(r, ht);
//                    }
//                }
//
//
//            for (int loc = 0; loc < weaponLocations.length; loc++) {
//                double locMultiplier = ASLocationMapper.damageLocationMultiplier(entity, loc, mount);
//                if (locMultiplier == 0) {
//                    continue;
//                }
//                for (int r = 0; r < element.getRangeBands(); r++) {
//                    double dam = baseDamage[r] * damageModifier * locMultiplier;
//                    if (dam > 0) {
//                        report.addLine(locationNames[loc] + ": " + mount.getName() + " " + "SMLE".charAt(r) + ": ",
//                                " Mul: " + damageModifier, "", dam);
//                    }
//                    if (!weapon.isCapital() && weapon.getBattleForceClass() != WeaponType.BFCLASS_TORP) {
//                        // Standard Damage
//                        weaponLocations[loc].addDamage(r, dam);
//                    }
//                    // Special Damage (SRM/LRM blocked by Artemis)
//                    if (!(MountedHelper.isAnyArtemis(mount.getLinkedBy()) && isSRMorLRMSpecial(weapon))) {
//                        if (weapon.getBattleForceClass() == WeaponType.BFCLASS_MML) {
//                            if (r == RANGE_BAND_SHORT) {
//                                weaponLocations[loc].addDamage(WeaponType.BFCLASS_SRM, r, dam);
//                            } else if (r == RANGE_BAND_MEDIUM) {
//                                weaponLocations[loc].addDamage(WeaponType.BFCLASS_SRM, r, dam / 2.0);
//                                weaponLocations[loc].addDamage(WeaponType.BFCLASS_LRM, r, dam / 2.0);
//                            } else {
//                                weaponLocations[loc].addDamage(WeaponType.BFCLASS_LRM, r, dam);
//                            }
//                        } else {
//                            weaponLocations[loc].addDamage(weapon.getBattleForceClass(), r, dam);
//                        }
//                    }
//                    if ((r == RANGE_BAND_LONG) && !(entity instanceof Aero) && weapon.hasAlphaStrikeIndirectFire()) {
//                        weaponLocations[loc].addIF(baseIFDamage * damageModifier * locMultiplier);
//                    }
//                }
//            }
//            if (entity instanceof Aero && weapon.isAlphaStrikePointDefense()) {
//                pointDefense += baseDamage[RANGE_BAND_SHORT] * damageModifier * ASLocationMapper.damageLocationMultiplier(entity, 0, mount);
//            }
//        }
//
//        if (entity instanceof BattleArmor) {
//            int vibroClaws = entity.countWorkingMisc(MiscType.F_VIBROCLAW);
//            if (vibroClaws > 0) {
//                weaponLocations[0].addDamage(0, vibroClaws);
//                weaponLocations[0].addDamage(WeaponType.BFCLASS_STANDARD, 0, vibroClaws);
//            }

//        }
//
//        if (entity instanceof Aero && ASConverter.roundUp(pointDefense) > 0) {
//            element.getSpecialAbilities().addSPA(PNT, ASConverter.roundUp(pointDefense));
//        }
//
//        adjustForHeat(conversionData);
//
//        // Standard HT
//        if (!element.isType(ASUnitType.CI)) {
//            int htS = resultingHTValue(weaponLocations[0].heatDamage.get(0));
//            int htM = resultingHTValue(weaponLocations[0].heatDamage.get(1));
//            int htL = resultingHTValue(weaponLocations[0].heatDamage.get(2));
//            if (htS + htM + htL > 0) {
//                element.getSpecialAbilities().addSPA(HT, ASDamageVector.createNormRndDmg(htS, htM, htL));
//            }
//        }
//
//        // IF (uses an ASDamageVector as value)
//        if (weaponLocations[0].getIF() > 0) {
//            element.getSpecialAbilities().addSPA(IF, ASDamageVector.createNormRndDmg(weaponLocations[0].getIF()).S);
//        }
//
//        // LRM, SRM, MML, TOR, AC, FLK, IATM, REL specials
//        for (int i = WeaponType.BFCLASS_LRM; i <= WeaponType.BFCLASS_REL; i++) {
//            // Aero do not get LRM/SRM/AC/IATM, FLK only in arcs
//            if ((entity instanceof Aero) && (i != WeaponType.BFCLASS_FLAK)) {
//                continue;
//            }
//            if (element.usesArcs() && (i == WeaponType.BFCLASS_FLAK)) {
//                List<Double> dmg = weaponLocations[0].specialDamage.get(i);
//                if (dmg != null) {
//                    element.getFrontArc().getSpecials().addSPA(FLK, ASDamageVector.createNormRndDmg(dmg, 4));
//                }
//                dmg = weaponLocations[1].specialDamage.get(i);
//                if (dmg != null) {
//                    element.getLeftArc().getSpecials().addSPA(FLK, ASDamageVector.createNormRndDmg(dmg, 4));
//                }
//                dmg = weaponLocations[2].specialDamage.get(i);
//                if (dmg != null) {
//                    element.getRightArc().getSpecials().addSPA(FLK, ASDamageVector.createNormRndDmg(dmg, 4));
//                }
//                dmg = weaponLocations[3].specialDamage.get(i);
//                if (dmg != null) {
//                    element.getRearArc().getSpecials().addSPA(FLK, ASDamageVector.createNormRndDmg(dmg, 4));
//                }
//                continue;
//            }
//            BattleForceSUA spa = getSUAForDmgClass(i);
//            List<Double> dmg = weaponLocations[0].specialDamage.get(i);
//            if ((dmg != null) && qualifiesForSpecial(dmg, spa)) {
//                if (spa == SRM) {
//                    element.getSpecialAbilities().addSPA(SRM, ASDamageVector.createNormRndDmgNoMin(dmg, 2));
//                } else if ((spa == LRM) || (spa == AC) || (spa == IATM)) {
//                    element.getSpecialAbilities().addSPA(spa, ASDamageVector.createNormRndDmgNoMin(dmg, element.getRangeBands()));
//                } else if ((spa == FLK) || (spa == TOR)) {
//                    element.getSpecialAbilities().addSPA(spa, ASDamageVector.createNormRndDmg(dmg, element.getRangeBands()));
//                } else if (spa == REL) {
//                    element.addSPA(spa);
//                }
//            }
//        }
//
//        // REL
//        if (weaponLocations[0].hasDamageClass(WeaponType.BFCLASS_REL)) {
//            element.addSPA(REL);
//        }
//
//        if (element.usesArcs()) {
//            // Large Aero (using Arcs)
//            setArcDamage(element, 0, element.getFrontArc());
//            setArcDamage(element, 1, element.getLeftArc());
//            setArcDamage(element, 2, element.getRightArc());
//            setArcDamage(element, 3, element.getRearArc());
//        } else {
//            // Standard damage
//            element.setStandardDamage(ASDamageVector.createUpRndDmg(
//                    weaponLocations[0].standardDamage, element.getRangeBands()));
//        }
//
//        // REAR damage
//        int rearLoc = getRearLocation(element);
//        if (rearLoc != -1 && weaponLocations[rearLoc].hasStandardDamage()) {
//            ASDamageVector rearDmg = ASDamageVector.createNormRndDmg(
//                    weaponLocations[rearLoc].standardDamage, element.getRangeBands());
//            element.getSpecialAbilities().addSPA(REAR, rearDmg);
//        }
//
//        // Turrets have SRM, LRM, AC, FLK, HT, TSEMP, TOR, AMS, TAG, ARTx, xNARC, IATM, REL
//        //TODO: 2 Turrets?
//        for (int loc = 0; loc < weaponLocations.length; loc++) {
//            if (turretsArcs.containsKey(locationNames[loc])) {
//                ASArcSummary arcTurret = turretsArcs.get(locationNames[loc]);
//                arcTurret.setStdDamage(ASDamageVector.createUpRndDmgMinus(weaponLocations[loc].standardDamage,
//                        element.getRangeBands()));
//                for (int i = WeaponType.BFCLASS_LRM; i <= WeaponType.BFCLASS_REL; i++) {
//                    BattleForceSUA spa = getSUAForDmgClass(i);
//                    List<Double> dmg = weaponLocations[loc].specialDamage.get(i);
//                    if ((dmg != null) && qualifiesForSpecial(dmg, spa)) {
//                        if (spa == SRM) {
//                            arcTurret.getSpecials().addSPA(spa, ASDamageVector.createNormRndDmgNoMin(dmg, 2));
//                        } else if ((spa == LRM) || (spa == TOR) || (spa == AC) || (spa == IATM)) {
//                            arcTurret.getSpecials().addSPA(spa, ASDamageVector.createNormRndDmgNoMin(dmg, element.getRangeBands()));
//                        } else if (spa == FLK) {
//                            arcTurret.getSpecials().addSPA(spa, ASDamageVector.createNormRndDmg(dmg, element.getRangeBands()));
//                        } else if (spa == REL) {
//                            arcTurret.getSpecials().addSPA(spa);
//                        }
//                    }
//                }
//                if (weaponLocations[loc].getIF() > 0) {
//                    arcTurret.getSpecials().addSPA(IF, ASDamageVector.createNormRndDmg(weaponLocations[loc].getIF()));
//                }
//                int htS = resultingHTValue(weaponLocations[loc].heatDamage.get(0));
//                int htM = resultingHTValue(weaponLocations[loc].heatDamage.get(1));
//                int htL = resultingHTValue(weaponLocations[loc].heatDamage.get(2));
//                if (htS + htM + htL > 0) {
//                    arcTurret.getSpecials().addSPA(HT, ASDamageVector.createNormRndDmg(htS, htM, htL));
//                }
//                if (!arcTurret.isEmpty()) {
//                    //TODO: If this is an arc??
//                    element.getSpecialAbilities().addSPA(TUR, arcTurret);
//                }
//            }
//        }
//
//    }
//
//    private static List<ASArcSummary> findArcTurret(AlphaStrikeElement element, Entity entity,
//                                                    Mounted mount, Map<String, ASArcSummary> arcsTurrets) {
//        var result = new ArrayList<ASArcSummary>();
//        for (int loc = 0; loc < weaponLocations.length; loc++) {
//            if (locationNames[loc].equals("TUR")
//                    && (ASLocationMapper.damageLocationMultiplier(entity, loc, mount) != 0)) {
//                result.add(arcsTurrets.get(locationNames[loc]));
//            }
//        }
//        return result;
//    }

    protected static boolean isArtilleryCannon(WeaponType weapon) {
        return (weapon.getAmmoType() == AmmoType.T_LONG_TOM_CANNON)
                || (weapon.getAmmoType() == AmmoType.T_SNIPER_CANNON)
                || (weapon.getAmmoType() == AmmoType.T_THUMPER_CANNON);
    }

    /** Translates an Artillery WeaponType to the AlphaStrike Special Unit Ability, if any can be found. */
    protected static BattleForceSUA getArtilleryType(WeaponType weaponType) {
        switch (weaponType.getAmmoType()) {
            case AmmoType.T_ARROW_IV:
                return (weaponType.getTechBase() == TECH_BASE_CLAN) ? ARTAC : ARTAIS;
            case AmmoType.T_LONG_TOM:
                return ARTLT;
            case AmmoType.T_SNIPER:
                return ARTS;
            case AmmoType.T_THUMPER:
                return ARTT;
            case AmmoType.T_LONG_TOM_CANNON:
                return ARTLTC;
            case AmmoType.T_SNIPER_CANNON:
                return ARTSC;
            case AmmoType.T_THUMPER_CANNON:
                return ARTTC;
            case AmmoType.T_CRUISE_MISSILE:
                switch (weaponType.getRackSize()) {
                    case 50:
                        return ARTCM5;
                    case 70:
                        return ARTCM7;
                    case 90:
                        return ARTCM9;
                    case 120:
                        return ARTCM12;
                }
            case AmmoType.T_BA_TUBE:
                return ARTBA;
        }
        return UNKNOWN;
    }

    /**
     * Returns true when the given weapon contributes to the LRM/SRM specials.
     */
    private static boolean isSRMorLRMSpecial(WeaponType weapon) {
        return weapon.getBattleForceClass() == WeaponType.BFCLASS_SRM
                || weapon.getBattleForceClass() == WeaponType.BFCLASS_LRM
                || weapon.getBattleForceClass() == WeaponType.BFCLASS_MML;
    }

    private static int resultingHTValue(int heatSum) {
        if (heatSum > 10) {
            return 2;
        } else if (heatSum > 4) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Adjusts all damage values for overheat, if applicable (i.e., if the unit tracks heat,
     * if its heat output is sufficiently over its heat dissipation and for L/E values, if
     * the prerequisites for OVL are fulfilled.
     * Also assigns OVL where applicable.
     */
    private void adjustForHeat() {
//        if (!entity.tracksHeat() && !element.usesArcs()) {
//            return;
//        }
//        int totalFrontHeat = getHeatGeneration(entity, element, false, false);
//        int heatCapacity = getHeatCapacity(entity, element);
//        report.addEmptyLine();
//        report.addSubHeader("Heat Adjustment:");
//        report.addLine("Heat Capacity: ", "", heatCapacity);
//        report.addLine("Total Heat M: ", "", totalFrontHeat);
//        if (totalFrontHeat - 4 <= heatCapacity) {
//            report.addLine("", "No adjustment needed", "");
//            return;
//        }
//
//        // Determine OV from the medium range damage
//        //TODO: this should not be necessary:
//        while (locations[0].standardDamage.size() < 4) {
//            weaponLocations[0].standardDamage.add(0.0);
//        }
//        double nonRounded = weaponLocations[0].standardDamage.get(RANGE_BAND_MEDIUM);
//        element.setOverheat(Math.min(heatDelta(nonRounded, heatCapacity, totalFrontHeat), 4));
//
//        report.addLine("Total Raw Dmg M: ", "", fmtDouble(nonRounded));
//        report.addLine("Total Adjusted Dmg M: ",
//                fmtDouble(nonRounded) + " x " + heatCapacity + " / (" + totalFrontHeat + " - 4)",
//                fmtDouble(nonRounded * heatCapacity / (totalFrontHeat - 4)));
//        report.addLine("Resulting Overheat", "OV " + element.getOverheat());
//
//        // Determine OVL from long range damage and heat
//        if (element.getOverheat() > 0 && element.usesOV()) {
//            double heatLong = getHeatGeneration(entity, element, false, true);
//            report.addLine("Total Heat L:", "", heatLong);
//            if (heatLong - 4 > heatCapacity) {
//                double nonRoundedL = weaponLocations[0].standardDamage.get(RANGE_BAND_LONG);
//                report.addLine("Total Damage L before Adjustment: ", "", nonRoundedL);
//                if (heatDelta(nonRoundedL, heatCapacity, heatLong) >= 1) {
//                    report.addLine("Resulting SPA", "OVL", "");
//                    element.addSPA(OVL);
//                }
//            }
//        }
//
//        // Adjust all weapon damages (E for units with OVL and arced units, M otherwise)
//        int maxAdjustmentRange = 1 + ((element.hasSUA(OVL) || element.usesArcs()) ? RANGE_BAND_EXTREME : RANGE_BAND_MEDIUM);
//        double frontadjustment = (double) heatCapacity / (totalFrontHeat - 4);
//        double rearHeat = getHeatGeneration(entity, element, true, false);
//        double rearAdjustment = rearHeat - 4 > heatCapacity ? heatCapacity / (rearHeat - 4) : 1;
//        for (int loc = 0; loc < weaponLocations.length; loc++) {
//            ASConverter.WeaponLocation wloc = weaponLocations[loc];
//            double adjustment = locationNames[loc].equals("REAR") ? rearAdjustment : frontadjustment;
//            for (int i = 0; i < Math.min(maxAdjustmentRange, wloc.standardDamage.size()); i++) {
//                wloc.standardDamage.set(i, heatAdjust(wloc.standardDamage.get(i), adjustment));
//            }
//            for (List<Double> damage : wloc.specialDamage.values()) {
//                for (int i = 0; i < Math.min(maxAdjustmentRange, damage.size()); i++) {
//                    damage.set(i, heatAdjust(damage.get(i), adjustment));
//                }
//            }
//            // IF is long-range fire; only adjust when the unit can overheat even in long range
//            if (element.hasSUA(OVL)) {
//                //TODO: really adjust this with round up to tenth?
//                wloc.indirect = heatAdjust(wloc.indirect, adjustment);
//            }
//        }
    }

    /**
     * Returns the delta between the unadjusted rounded and heat-adjusted rounded damage value,
     * according to ASC - Converting Heat Errata v1.2.
     * Use only for determining if a unit has OV or OVL.
     */
    private static int heatDelta(double damage, int heatCapacity, double heat) {
        int roundedUp = ASConverter.roundUp(roundUpToTenth(damage));
        int roundedUpAdjustedL = ASConverter.roundUp(roundUpToTenth(damage * heatCapacity / (heat - 4)));
        return roundedUp - roundedUpAdjustedL;
    }

    private static double heatAdjust(double value, double adjustment) {
        return roundUpToTenth(value * adjustment);
    }

    /**
     * Returns true when the heat-adjusted and tenth-rounded damage values in the List allow the given spa.
     * Only used for the damage specials LRM, SRM, TOR, IATM, AC, FLK
     */
    private static boolean qualifiesForSpecial(List<Double> dmg, BattleForceSUA spa) {
        if (((spa == FLK) || (spa == TOR)) && dmg.stream().mapToDouble(Double::doubleValue).sum() > 0) {
            return true;
        } else {
            return (dmg.size() > 1) && (dmg.get(1) >= 1);
        }
    }

    /**
     * Returns the given number, rounded up to the nearest tenth. ASC Converting Heat Errata v1.2:
     * A value of 0.401 -> 0.5; i.e. any fraction will make it round up, not just the second digit.
     */
    public static double roundUpToTenth(double number) {
        double intermediate = 10 * number; // 0.401 -> 4.01 or 4.00999999 or 4.010000001
        double result = (int) intermediate; // 4
        if (intermediate - (int) intermediate > 0.000001) { // -> 0.01 or 0.009999 or 0.01000001
            result += 1; // 5
        }
        return result / 10; // 0.5 or 0.4999999999

    }

//    private static int getRearLocation(AlphaStrikeElement element) {
//        for (int loc = 0; loc < weaponLocations.length; loc++) {
//            if (locationNames[loc].equals("REAR")) {
//                return loc;
//            }
//        }
//        return -1;
//    }

    /**
     * Returns the total generated heat (weapons and movement) for a Mek or Aero for the purpose of finding OV / OVL values.
     * If onlyRear is true, only rear-facing weapons are included, otherwise only front-facing weapons are included!
     * If onlyLongRange is true, only weapons with an L damage value are included.
     */
    protected int getHeatGeneration(boolean onlyRear, boolean onlyLongRange) {
        if (entity instanceof Mech) {
            return getMekHeatGeneration((Mech) entity, element, onlyRear, onlyLongRange);
        } else {
            return 0;
        }
    }

    /**
     * Returns the total generated heat (weapons and movement) for a Mech for the purpose of finding OV / OVL values.
     * If onlyRear is true, rear-facing weapons are included, otherwise only front-facing weapons are included!
     * If onlyLongRange is true, only weapons with an L damage value are included.
     */
    private static int getMekHeatGeneration(Mech entity, AlphaStrikeElement element, boolean onlyRear, boolean onlyLongRange) {
        int totalHeat = 0;

        if (entity.getJumpMP() > 0) {
            totalHeat += getJumpHeat(entity, element);
        } else if (!entity.isIndustrial() && entity.hasEngine()) {
            totalHeat += entity.getEngine().getRunHeat(entity);
        }

        for (Mounted mount : entity.getWeaponList()) {
            totalHeat += mekWeaponHeat(mount, onlyRear, onlyLongRange);
        }

        if (entity.hasWorkingMisc(MiscType.F_STEALTH, -1)
                || entity.hasWorkingMisc(MiscType.F_VOIDSIG, -1)
                || entity.hasWorkingMisc(MiscType.F_NULLSIG, -1)) {
            totalHeat += 10;
        }

        if (entity.hasWorkingMisc(MiscType.F_CHAMELEON_SHIELD, -1)) {
            totalHeat += 6;
        }

        return totalHeat;
    }


    private static int mekWeaponHeat(Mounted weapon, boolean onlyRear, boolean onlyLongRange) {
        WeaponType weaponType = (WeaponType) weapon.getType();
        if (weaponType.hasFlag(WeaponType.F_ONESHOT)
                || (onlyRear && !weapon.isRearMounted())
                || (!onlyRear && weapon.isRearMounted())
                || (onlyLongRange && weaponType.getBattleForceDamage(LONG_RANGE) == 0)) {
            return 0;
        } else {
            return weaponHeat(weaponType);
        }
    }

    private static int getJumpHeat(Entity entity, AlphaStrikeElement element) {
        if ((entity.getJumpType() == Mech.JUMP_IMPROVED)
                && (entity.getEngine().getEngineType() == Engine.XXL_ENGINE)) {
            return Math.max(3, element.getJumpMove() / 2);
        } else if (entity.getJumpType() == Mech.JUMP_IMPROVED) {
            return Math.max(3, ASConverter.roundUp(0.25 * element.getJumpMove()));
        } else if (entity.getEngine().getEngineType() == Engine.XXL_ENGINE) {
            return Math.max(6, element.getJumpMove());
        } else {
            return Math.max(3, element.getJumpMove() / 2);
        }
    }

    protected static int weaponHeat(WeaponType weaponType) {
        int ammoType = weaponType.getAmmoType();
        if (ammoType == AmmoType.T_AC_ROTARY) {
            return weaponType.getHeat() * 6;
        } else if ((ammoType == AmmoType.T_AC_ULTRA) || (ammoType == AmmoType.T_AC_ULTRA_THB)) {
            return weaponType.getHeat() * 2;
        } else {
            return weaponType.getHeat();
        }
    }

    /** Returns the heat dissipation for Meks and AFs, according to ASC - Converting Heat Errata v1.2. */
    private int getHeatCapacity() {
        int heatCapacity = 0;
        if (entity instanceof Mech) {
            heatCapacity = ((Mech) entity).getHeatCapacity(false, false);
        } else if (entity.isFighter() || element.usesArcs()) {
            heatCapacity = entity.getHeatCapacity(false);
        }
//        report.addLine("Heat Sink Capacity:", "", heatCapacity);
        long coolantPodCount = entity.getEquipment().stream().filter(Mounted::isCoolantPod).count();
        if (coolantPodCount > 0) {
            heatCapacity += coolantPodCount;
//            report.addLine("Coolant Pods", "+ ", coolantPodCount);
        }
        if (entity.hasWorkingMisc(MiscType.F_PARTIAL_WING)) {
            heatCapacity += 3;
//            report.addLine("Partial Wing", "+ 3");
        }
        if (element.hasSUA(RHS)) {
            heatCapacity += 1;
//            report.addLine("Radical Heat Sinks", "+ 1");
        }
        if (element.hasSUA(ECS)) {
            heatCapacity += 1;
//            report.addLine("Emergency Coolant System", "+ 1");
        }
        return heatCapacity;
    }

    private static void addSPAToArcs(BattleForceSUA spa, Mounted mount, Entity entity, AlphaStrikeElement element) {
        if (ASLocationMapper.damageLocationMultiplier(entity, 0, mount) != 0) {
            element.getFrontArc().getSpecials().addSPA(spa);
        }
        if (ASLocationMapper.damageLocationMultiplier(entity, 1, mount) != 0) {
            element.getLeftArc().getSpecials().addSPA(spa);
        }
        if (ASLocationMapper.damageLocationMultiplier(entity, 2, mount) != 0) {
            element.getRightArc().getSpecials().addSPA(spa);
        }
        if (ASLocationMapper.damageLocationMultiplier(entity, 3, mount) != 0) {
            element.getRearArc().getSpecials().addSPA(spa);
        }
    }

//    private static void setArcDamage(AlphaStrikeElement element, int loc, ASArcSummary arc) {
//        ASConverter.WeaponLocation wpLoc = weaponLocations[loc];
//        Map<Integer, List<Double>> specialDmg = wpLoc.specialDamage;
//        arc.setStdDamage(ASDamageVector.createUpRndDmg(wpLoc.standardDamage, 4));
//        List<Double> scap = specialDmg.getOrDefault(BFCLASS_SUBCAPITAL, ZERO_DAMAGE);
//        arc.setSCAPDamage(ASDamageVector.createUpRndDmgMinus(scap, 4));
//        List<Double> cap = specialDmg.getOrDefault(BFCLASS_CAPITAL, ZERO_DAMAGE);
//        arc.setCAPDamage(ASDamageVector.createUpRndDmgMinus(cap, 4));
//        List<Double> msl = specialDmg.getOrDefault(BFCLASS_CAPITAL_MISSILE, ZERO_DAMAGE);
//        arc.setMSLDamage(ASDamageVector.createUpRndDmgMinus(msl, 4));
//    }

    private static BattleForceSUA getSUAForDmgClass(int dmgClass) {
        switch (dmgClass) {
            case WeaponType.BFCLASS_LRM:
                return LRM;
            case WeaponType.BFCLASS_SRM:
                return SRM;
            case WeaponType.BFCLASS_AC:
                return AC;
            case WeaponType.BFCLASS_FLAK:
                return FLK;
            case WeaponType.BFCLASS_IATM:
                return IATM;
            case WeaponType.BFCLASS_TORP:
                return TOR;
            case WeaponType.BFCLASS_REL:
                return REL;
            default:
                return UNKNOWN;
        }
    }
}
