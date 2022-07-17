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
import megamek.common.annotations.Nullable;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.common.weapons.other.CLFussilade;

import java.util.*;

import static megamek.common.ITechnology.TECH_BASE_CLAN;
import static megamek.common.alphaStrike.AlphaStrikeElement.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;
import static megamek.client.ui.swing.calculationReport.CalculationReport.fmtDouble;

public class ASDamageConverter2 {

    // AP weapon mounts have a set damage value.
    private static final double AP_MOUNT_DAMAGE = 0.05;
    private static final int[] TROOP_FACTOR = {
            0, 0, 1, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12,
            13, 14, 15, 16, 16, 17, 17, 17, 18, 18
    };
    private static final List<Double> ZERO_DAMAGE = List.of(0d, 0d, 0d, 0d);

    private Entity entity;
    private AlphaStrikeElement element;
    private CalculationReport report;

//    private ASConverter.WeaponLocation[] weaponLocations;
    private String[] locationNames;
    private int[] heat;
    // The locations where damage and special abilities are going to end up in. [0] contains the unit's standard
    // damage and central abilities, the others can be turrets, arcs or rear.
    private ASArcSummary[] locations;
    private Map<WeaponType, Boolean> ammoForWeapon = new HashMap<>();
    private final boolean hasTargetingComputer;


    ASDamageConverter2(ASConverter.ConversionData conversionData) {
        entity = conversionData.entity;
        element = conversionData.element;
        report = conversionData.conversionReport;
        locations = new ASArcSummary[ASLocationMapper.damageLocationsCount(entity)];
        locationNames = new String[locations.length];
        for (int index = 0; index < locationNames.length; index++) {
            locationNames[index] = ASLocationMapper.locationName(entity, index);
            if (element.usesArcs()) {
                locations[index] = ASArcSummary.createArcSummary(element);
            } else {
                // If it is not a turret, it can still use the turret-type summary
                locations[index] = ASArcSummary.createTurretSummary(element);
            }
        }
        hasTargetingComputer = entity.hasTargComp();
    }

    /*
    void convertDamage2() {

        No Arcs:
        Loop Weapons
           AddMDamage
        FindOV / Find Adjustment
        Loop Weapons
               AddLDamage
        AF/BM:FindOVL / Adjust
        Loop Weapons
               AddSDamage
               Adjust
        Aero:Loop Weapons
               AddEDamage
               Adjust

        Assign to real fields


        Arcs:
        Loop Weapons
           Loop Bayweapons
               Loop Arcs (Wegen report)
                   AddSDamage
                       STD
                       CAP
                       SCAP
                       MSL
                   AddMDamage
                   ...
                   AddLDamage
                   ...
                   AddEDamage
                   ...
        Adjust for all heat
        Assign to real fields





    }
    */

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

        ArrayList<Mounted> weaponsList = new ArrayList<>(entity.getWeaponList());
        weaponsList.removeIf(Objects::isNull);

        // Special abilities
        report.addEmptyLine();
        report.addLine("--- Special Abilities:", "");
        for (Mounted weapon : weaponsList) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            assignSpecialAbilities(weapon, weaponType);
        }

        // Heat
        if (element.usesOV()) {
            report.addEmptyLine();
            report.addLine("--- Heat Capacity:", "");
            int heatCapacity = getHeatCapacity(entity, element);
        }

        // Front S damage
        report.addEmptyLine();
        report.addLine("--- Short Range Damage:", "");
        double rawSDamage = calculateFrontDamage(weaponsList, SHORT_RANGE);
        report.addLine("Raw S damage:", "= " + fmtDouble(rawSDamage));
        if (element.usesOV()) {
        // Heat adjust
        }
        int finalSDamage = ASConverter.roundUp(roundUpToTenth(rawSDamage));
        report.addLine("Final S damage:", "Dual Rounding", "= " + fmtDouble(finalSDamage));

        // Front M damage
        report.addEmptyLine();
        report.addLine("--- Medium Range Damage:", "");
        double rawMDamage = calculateFrontDamage(weaponsList, MEDIUM_RANGE);
        report.addLine("Raw M damage:", "= " + fmtDouble(rawMDamage));
        if (element.usesOV()) {
            // Heat adjust
        }
        int finalMDamage = ASConverter.roundUp(roundUpToTenth(rawMDamage));
        report.addLine("Final M damage:", "Dual Rounding", "= " + fmtDouble(finalMDamage));

        // Front L damage
        report.addEmptyLine();
        report.addLine("--- Long Range Damage:", "");
        double rawLDamage = calculateFrontDamage(weaponsList, LONG_RANGE);
        report.addLine("Raw L damage:", "= " + fmtDouble(rawLDamage));
        if (element.usesOV()) {
            // Heat adjust
        }
        int finalLDamage = ASConverter.roundUp(roundUpToTenth(rawLDamage));
        report.addLine("Final L damage:", "Dual Rounding", "= " + fmtDouble(finalLDamage));

        // Front E damage
        if (element.usesSMLE()) {
            report.addEmptyLine();
            report.addLine("--- Extreme Range Damage:", "");
            double rawEDamage = calculateFrontDamage(weaponsList, EXTREME_RANGE);
            report.addLine("Raw E damage:", "= " + fmtDouble(rawEDamage));
            if (element.usesOV()) {
                // Heat adjust
            }
            int finalEDamage = ASConverter.roundUp(roundUpToTenth(rawEDamage));
            report.addLine("Final E damage:", "Dual Rounding", "= " + fmtDouble(finalEDamage));
        }


    }

    private double calculateFrontDamage(List<Mounted> weaponsList, int range) {
        double rawDamage = 0;
        for (Mounted weapon : weaponsList) {
            double damageModifier = 1;
            WeaponType weaponType = (WeaponType) weapon.getType();
            double baseDamage = weaponType.getBattleForceDamage(range);
            if ((ASLocationMapper.damageLocationMultiplier(entity, 0, weapon) > 0) && (baseDamage > 0)) {
                damageModifier = getDamageMultiplier(weapon, weaponType);
                double modifiedDamage = baseDamage * damageModifier;
                String calculation = (damageModifier != 1) ? baseDamage + " x " + damageModifier : "";
                report.addLine(weapon.getName(), calculation, "+ " + fmtDouble(modifiedDamage));
                rawDamage += modifiedDamage;
            }
        }
        return rawDamage;
    }

    private double getDamageMultiplier(Mounted weapon, WeaponType weaponType) {
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

    private void assignSpecialAbilities(Mounted weapon, WeaponType weaponType) {
        if (weaponType.getAmmoType() == AmmoType.T_INARC) {
            report.addLine(weapon.getName(), "INARC1");
            assignToLocation(weapon, INARC, 1);
        } else if (weaponType.getAmmoType() == AmmoType.T_NARC) {
            if (weaponType.hasFlag(WeaponType.F_BA_WEAPON)) {
                report.addLine(weapon.getName(), "CNARC1");
                assignToLocation(weapon, CNARC, 1);
            } else {
                report.addLine(weapon.getName(), "SNARC1");
                assignToLocation(weapon, SNARC, 1);
            }
        }

        //            if (weapon.getAtClass() == WeaponType.CLASS_SCREEN) {
//                element.getSpecialAbilities().addSPA(SCR, 1);
//                continue;
//            }
//
//            //TODO: really not Aero? Where is AMS pointdefense handled else?
//            if (weapon.hasFlag(WeaponType.F_AMS)) {
//                if (ASLocationMapper.damageLocationMultiplier(entity, 0, mount) > 0) {
//                    if (entity instanceof Aero) {
//                        pointDefense += 0.3;
//                    } else {
//                        element.addSPA(AMS);
//                    }
//                }
//                for (ASArcSummary arcTurret : findArcTurret(element, entity, mount, turretsArcs)) {
//                    if (entity instanceof Aero) {
//                        arcTurret.getSpecials().addSPA(PNT, 0.3);
//                    } else {
//                        arcTurret.getSpecials().addSPA(AMS);
//                    }
//                }
//                continue;
//            }
//
//            if (weapon.hasFlag(WeaponType.F_TAG)) {
//                if (weapon.hasFlag(WeaponType.F_C3MBS)) {
//                    element.getSpecialAbilities().addSPA(C3BSM, 1);
//                    element.getSpecialAbilities().addSPA(MHQ, 6);
//                } else if (weapon.hasFlag(WeaponType.F_C3M)) {
//                    element.getSpecialAbilities().addSPA(C3M, 1);
//                    element.getSpecialAbilities().addSPA(MHQ, 5);
//                }
//                if (weapon.getShortRange() < 5) {
//                    element.addSPA(LTAG);
//                    findArcTurret(element, entity, mount, turretsArcs).forEach(a -> a.getSpecials().addSPA(LTAG));
//                } else {
//                    element.addSPA(TAG);
//                    findArcTurret(element, entity, mount, turretsArcs).forEach(a -> a.getSpecials().addSPA(TAG));
//                }
//                continue;
//            }
//
//            if (weapon.hasFlag(WeaponType.F_FLAMER) || weapon.hasFlag(WeaponType.F_PLASMA)) {
//                // For CI, add a placeholder heat value to be replaced later by the S damage value, ASC p.124
//                element.getSpecialAbilities().addSPA(HT, ASDamageVector.createNormRndDmg(0));
//            }
//
//            if (weapon.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
//                if (!((entity instanceof Aero) && isArtilleryCannon(weapon))) {
//                    element.getSpecialAbilities().addSPA(getArtilleryType(weapon), 1);
//                    findArcTurret(element, entity, mount, turretsArcs).forEach(a -> a.getSpecials().addSPA(getArtilleryType(weapon), 1));
//                    continue;
//                }
//            }
//
//            if (weapon instanceof ArtilleryBayWeapon) {
//                for (int index : mount.getBayWeapons()) {
//                    Mounted m = entity.getEquipment(index);
//                    if (m.getType() instanceof WeaponType) {
//                        element.getSpecialAbilities().addSPA(getArtilleryType((WeaponType) m.getType()), 1);
//                        findArcTurret(element, entity, mount, turretsArcs)
//                                .forEach(a -> a.getSpecials().addSPA(getArtilleryType((WeaponType) m.getType()), 1));
//                    }
//                }
//            }
//
//            if (weapon.getAmmoType() == AmmoType.T_BA_MICRO_BOMB) {
//                bombRacks++;
//                continue;
//            }
//
//            if (weapon.getAmmoType() == AmmoType.T_TASER) {
//                if (entity instanceof BattleArmor) {
//                    element.getSpecialAbilities().addSPA(BTAS, 1);
//                } else {
//                    element.getSpecialAbilities().addSPA(MTAS, 1);
//                }
//            }
//
//            if (weapon.hasFlag(WeaponType.F_TSEMP)) {
//                BattleForceSUA spa = weapon.hasFlag(WeaponType.F_ONESHOT) ? TSEMPO : TSEMP;
//                element.getSpecialAbilities().addSPA(spa, 1);
//                findArcTurret(element, entity, mount, turretsArcs).forEach(a -> a.getSpecials().addSPA(spa, 1));
//                continue;
//            }
//
//            if (weapon instanceof InfantryAttack) {
//                element.addSPA(AM);
//                continue;
//            }

    }

    private void assignToLocation(Mounted weapon, BattleForceSUA sua, int number) {
        for (int i = 0; i < locations.length; i++) {
            if (ASLocationMapper.damageLocationMultiplier(entity, i, weapon) != 0) {
                locations[i].getSpecials().addSPA(sua, number);
            }
        }
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
//            if (weapon instanceof BayWeapon) {
//                for (int index : mount.getBayWeapons()) {
//                    Mounted m = entity.getEquipment(index);
//                    if (m.getType() instanceof WeaponType) {
//                        if (m.getType().hasFlag(WeaponType.F_AMS)) {
//                            if (ASLocationMapper.damageLocationMultiplier(entity, 0, m) != 0) {
//                                element.getFrontArc().getSpecials().addSPA(PNT, 0.3d);
//                            }
//                            if (ASLocationMapper.damageLocationMultiplier(entity, 1, m) != 0) {
//                                element.getLeftArc().getSpecials().addSPA(PNT, 0.3d);
//                            }
//                            if (ASLocationMapper.damageLocationMultiplier(entity, 2, m) != 0) {
//                                element.getRightArc().getSpecials().addSPA(PNT, 0.3d);
//                            }
//                            if (ASLocationMapper.damageLocationMultiplier(entity, 3, m) != 0) {
//                                element.getRearArc().getSpecials().addSPA(PNT, 0.3d);
//                            }
//                            continue;
//                        }
//
//                        if (((WeaponType) m.getType()).isAlphaStrikePointDefense()) {
//                            double sDmg = ((WeaponType) m.getType()).getBattleForceDamage(ranges[0], m.getLinkedBy());
//                            if (ASLocationMapper.damageLocationMultiplier(entity, 0, m) != 0) {
//                                element.getFrontArc().getSpecials().addSPA(PNT, sDmg);
//                            }
//                            if (ASLocationMapper.damageLocationMultiplier(entity, 1, m) != 0) {
//                                element.getLeftArc().getSpecials().addSPA(PNT, sDmg);
//                            }
//                            if (ASLocationMapper.damageLocationMultiplier(entity, 2, m) != 0) {
//                                element.getRightArc().getSpecials().addSPA(PNT, sDmg);
//                            }
//                            if (ASLocationMapper.damageLocationMultiplier(entity, 3, m) != 0) {
//                                element.getRearArc().getSpecials().addSPA(PNT, sDmg);
//                            }
//                        }
//
//                        // TELE missile special ability for Large Aero
//                        if ((((WeaponType) m.getType()).getAtClass() == WeaponType.CLASS_TELE_MISSILE)) {
//                            addSPAToArcs(TELE, m, entity, element);
//                        }
//
//                        // MSL missile special ability for Large Aero
//                        if (!element.usesCapitalWeapons() && element.isAerospace()
//                                && ((WeaponType) m.getType()).getBattleForceClass() == WeaponType.BFCLASS_CAPITAL_MISSILE) {
//                            addSPAToArcs(MSL, m, entity, element);
//                        }
//
//                        // SCAP missile special ability for Large Aero
//                        if (!element.usesCapitalWeapons() && element.isAerospace()
//                                && ((WeaponType) m.getType()).getBattleForceClass() == WeaponType.BFCLASS_SUBCAPITAL) {
//                            addSPAToArcs(SCAP, m, entity, element);
//                        }
//
//
//                        for (int r = 0; r < element.getRangeBands(); r++) {
//                            baseDamage[r] += ((WeaponType) m.getType()).getBattleForceDamage(ranges[r], m.getLinkedBy());
//                            heat[r] += ((WeaponType) m.getType()).getBattleForceHeatDamage(ranges[r]);
//                            for (int loc = 0; loc < weaponLocations.length; loc++) {
//                                double locMultiplier = ASLocationMapper.damageLocationMultiplier(entity, loc, m);
//                                if (locMultiplier != 0) {
//                                    weaponLocations[loc].addDamage(
//                                            ((WeaponType) m.getType()).getBattleForceClass(), r, baseDamage[r] * locMultiplier);
//                                }
//                            }
//                        }
//                    }
//                }
//            } else {
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
//            }
//
//            // Targetting Computer
//            if (hasTC && weapon.hasFlag(WeaponType.F_DIRECT_FIRE)
//                    && (weapon.getAmmoType() != AmmoType.T_AC_LBX)
//                    && (weapon.getAmmoType() != AmmoType.T_AC_LBX_THB)) {
//                damageModifier *= 1.10;
//            }
//
//            // Actuator Enhancement System
//            if (entity.hasWorkingMisc(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM, -1, mount.getLocation())
//                    && ((mount.getLocation() == Mech.LOC_LARM) || (mount.getLocation() == Mech.LOC_RARM))) {
//                damageModifier *= 1.05;
//            }
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
//        if (entity instanceof Infantry) {
//            Infantry infantry = (Infantry) entity;
//            int baseRange = 0;
//            if ((infantry.getSecondaryWeapon() != null) && (infantry.getSecondaryN() >= 2)) {
//                baseRange = infantry.getSecondaryWeapon().getInfantryRange();
//            } else if (infantry.getPrimaryWeapon() != null) {
//                baseRange = infantry.getPrimaryWeapon().getInfantryRange();
//            }
//            int range = baseRange * 3;
//            for (int r = 0; r < STANDARD_RANGES.length; r++) {
//                if (range >= STANDARD_RANGES[r]) {
//                    weaponLocations[0].addDamage(r, getConvInfantryStandardDamage(infantry));
//                } else {
//                    break;
//                }
//            }
//        }
//
//        if (entity instanceof BattleArmor) {
//            int vibroClaws = entity.countWorkingMisc(MiscType.F_VIBROCLAW);
//            if (vibroClaws > 0) {
//                weaponLocations[0].addDamage(0, vibroClaws);
//                weaponLocations[0].addDamage(WeaponType.BFCLASS_STANDARD, 0, vibroClaws);
//            }
//            if (bombRacks > 0) {
//                element.getSpecialAbilities().addSPA(BOMB, (bombRacks * ((BattleArmor) entity).getShootingStrength()) / 5);
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

    private static boolean isArtilleryCannon(WeaponType weapon) {
        return (weapon.getAmmoType() == AmmoType.T_LONG_TOM_CANNON)
                || (weapon.getAmmoType() == AmmoType.T_SNIPER_CANNON)
                || (weapon.getAmmoType() == AmmoType.T_THUMPER_CANNON);
    }

    /** Translates an Artillery WeaponType to the AlphaStrike Special Unit Ability, if any can be found. */
    private static @Nullable
    BattleForceSUA getArtilleryType(WeaponType weaponType) {
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
        return null;
    }

    private static double getBattleArmorDamage(WeaponType weapon, int range, BattleArmor ba, boolean apmMount) {
        double dam = 0;
        if (apmMount) {
            if (range == 0) {
                dam = AP_MOUNT_DAMAGE;
            }
        } else {
            dam = weapon.getBattleForceDamage(range);
        }
        return dam * (TROOP_FACTOR[Math.min(ba.getShootingStrength(), 30)] + 0.5);
    }

    /**
     * Returns true when the given weapon contributes to the LRM/SRM specials.
     */
    private static boolean isSRMorLRMSpecial(WeaponType weapon) {
        return weapon.getBattleForceClass() == WeaponType.BFCLASS_SRM
                || weapon.getBattleForceClass() == WeaponType.BFCLASS_LRM
                || weapon.getBattleForceClass() == WeaponType.BFCLASS_MML;
    }

    private static double getConvInfantryStandardDamage(Infantry inf) {
        return inf.getDamagePerTrooper() * TROOP_FACTOR[Math.min(inf.getShootingStrength(), 30)] / 10.0;
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
//    private static void adjustForHeat(ASConverter.ConversionData conversionData) {
//        AlphaStrikeElement element = conversionData.element;
//        Entity entity = conversionData.entity;
//        CalculationReport report = conversionData.conversionReport;
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
//        // No special treatment for the rear arc of Large Aero units
//        if (element.usesArcs()) {
//            rearAdjustment = frontadjustment;
//            if (element.getFrontArc().hasSPA(PNT)) {
//                double pntValue = (Double) element.getFrontArc().getSPA(PNT);
//                element.getFrontArc().getSpecials().replaceSPA(PNT, pntValue / frontadjustment);
//            }
//            if (element.getLeftArc().hasSPA(PNT)) {
//                double pntValue = (Double) element.getLeftArc().getSPA(PNT);
//                element.getLeftArc().getSpecials().replaceSPA(PNT, pntValue / frontadjustment);
//            }
//            if (element.getRightArc().hasSPA(PNT)) {
//                double pntValue = (Double) element.getRightArc().getSPA(PNT);
//                element.getRightArc().getSpecials().replaceSPA(PNT, pntValue / frontadjustment);
//            }
//            if (element.getRearArc().hasSPA(PNT)) {
//                double pntValue = (Double) element.getRearArc().getSPA(PNT);
//                element.getRearArc().getSpecials().replaceSPA(PNT, pntValue / frontadjustment);
//            }
//        }
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
//    }

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
    private static int getHeatGeneration(Entity entity, AlphaStrikeElement element,
                                         boolean onlyRear, boolean onlyLongRange) {
        if (entity instanceof Mech) {
            return getMekHeatGeneration((Mech) entity, element, onlyRear, onlyLongRange);
        } else {
            return getAeroHeatGeneration((Aero) entity, onlyRear, onlyLongRange);
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

    /**
     * Returns the total generated heat (weapons) for an Aero for heat correction, OV and OVL.
     * If onlyRear is true, rear-facing weapons are included, otherwise only front-facing weapons are included!
     * If onlyLongRange is true, only weapons with an L damage value are included.
     */
    private static int getAeroHeatGeneration(Aero entity, boolean onlyRear, boolean onlyLongRange) {
        int totalHeat = entity.hasWorkingMisc(MiscType.F_STEALTH, -1) ? 10 : 0;

        for (Mounted mount : entity.getWeaponList()) {
            WeaponType weapon = (WeaponType) mount.getType();
            if (weapon instanceof BayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    totalHeat += aeroWeaponHeat(entity.getEquipment(index), onlyRear, onlyLongRange);
                }
            } else {
                totalHeat += aeroWeaponHeat(mount, onlyRear, onlyLongRange);
            }
        }
        return totalHeat;
    }

    private static int aeroWeaponHeat(Mounted weapon, boolean onlyRear, boolean onlyLongRange) {
        WeaponType weaponType = (WeaponType) weapon.getType();
        if (weaponType.hasFlag(WeaponType.F_ONESHOT)
                || (onlyRear && !weapon.isRearMounted() && (weapon.getLocation() != Aero.LOC_AFT))
                || (!onlyRear && (weapon.isRearMounted() || (weapon.getLocation() == Aero.LOC_AFT)))
                || (onlyLongRange && weaponType.getBattleForceDamage(LONG_RANGE) == 0)) {
            return 0;
        } else {
            return weaponHeat(weaponType);
        }
    }

    private static int weaponHeat(WeaponType weaponType) {
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
    private int getHeatCapacity(Entity entity, AlphaStrikeElement element) {
        int heatCapacity = 0;
        if (entity instanceof Mech) {
            heatCapacity = ((Mech) entity).getHeatCapacity(false, false);
        } else if (entity.isFighter()) {
            heatCapacity = entity.getHeatCapacity(false);
        } else if (element.usesArcs()) {
            heatCapacity = entity.getHeatCapacity(false);
        }
        report.addLine("Heat Sink Capacity:", "", heatCapacity);
        long coolantPodCount = entity.getEquipment().stream().filter(Mounted::isCoolantPod).count();
        if (coolantPodCount > 0) {
            heatCapacity += coolantPodCount;
            report.addLine("Coolant Pods", "+ ", coolantPodCount);
        }
        if (entity.hasWorkingMisc(MiscType.F_PARTIAL_WING)) {
            heatCapacity += 3;
            report.addLine("Partial Wing", "+ 3");
        }
        if (element.hasSUA(RHS)) {
            heatCapacity += 1;
            report.addLine("Radical Heat Sinks", "+ 1");
        }
        if (element.hasSUA(ECS)) {
            heatCapacity += 1;
            report.addLine("Emergency Coolant System", "+ 1");
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
