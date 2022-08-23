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
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.alphaStrike.*;
import megamek.common.weapons.bayweapons.BayWeapon;

import java.util.*;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.alphaStrike.AlphaStrikeElement.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASArcedDamageConverter extends ASAeroDamageConverter {

    private Map<Mounted, Integer> collectedWeapons = new HashMap<>();


    protected ASArcedDamageConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
        for (int index = 0; index < locationNames.length; index++) {
            locations[index] = ASArcSummary.createArcSummary(element);
        }
        List<Mounted> flattenedWeaponList = new ArrayList<>();
        for (Mounted weapon : entity.getWeaponList()) {
            if (weapon.getType() instanceof BayWeapon) {
                weapon.getBayWeapons().stream().map(entity::getEquipment).forEach(flattenedWeaponList::add);
            } else {
                flattenedWeaponList.add(weapon);
            }
        }
        weaponsList = flattenedWeaponList;
        weaponsList.removeIf(Objects::isNull);
        // Generate a Map of weapon and weapon count where the keys are equal for conversion purposes
        for (Mounted weapon : weaponsList) {
            boolean foundKey = false;
            for (Mounted presentKey : collectedWeapons.keySet()) {
                if (areConversionEqual(weapon, presentKey)) {
                    collectedWeapons.merge(presentKey, 1, Integer::sum);
                    foundKey = true;
                    break;
                }
            }
            if (!foundKey) {
                collectedWeapons.put(weapon, 1);
            }
        }
    }

    /** @return True when the two weapons are equal for conversion purposes (same type, location and links). */
    private boolean areConversionEqual(Mounted weapon1, Mounted weapon2) {
        return weapon1.getType().equals(weapon2.getType())
                && weapon1.getLocation() == weapon2.getLocation()
                && weapon1.isRearMounted() == weapon2.isRearMounted()
                && ((weapon1.getLinkedBy() == null && weapon2.getLinkedBy() == null)
                || (weapon1.getLinkedBy() != null
                && weapon1.getLinkedBy().getType().equals(weapon2.getLinkedBy().getType())));
    }

    @Override
    protected void processDamage() {
        calculateHeatAdjustment();
        processArcs();
    }

    protected void calculateHeatAdjustment() {
        report.addLine("--- Heat Adjustment", "", "");
        int heatCapacity = getHeatCapacity();
        int totalHeat = getHeatGeneration(false, false);
        report.addLine("Heat Capacity", heatCapacity + "");
        report.addLine("Heat Generation", totalHeat + "");
        if (totalHeat - 4 > heatCapacity) {
            needsHeatAdjustment = true;
            heatAdjustFactorSM = (double) heatCapacity / (totalHeat - 4);
            report.addLine("Adjustment factor", heatCapacity + " / (" + totalHeat + " - 4)",
                    "= " + formatForReport(heatAdjustFactorSM));
        } else {
            report.addLine("", "No heat adjustment");
        }
    }

    private void processArcs() {
        processArc(ASArcs.FRONT);
        processArc(ASArcs.LEFT);
        processArc(ASArcs.RIGHT);
        processArc(ASArcs.REAR);
    }

    private void processArc(ASArcs arc) {
        // UNKNOWN is used as a placeholder to calculate STD damage
        processArcSpecial(arc, STD);
        processArcSpecial(arc, MSL);
        processArcSpecial(arc, CAP);
        processArcSpecial(arc, SCAP);
        processArcSpecial(arc, FLK);
    }

    @Override
    protected int weaponHeat(Mounted weapon, boolean onlyRear, boolean onlyLongRange) {
        return weaponHeat((WeaponType) weapon.getType());
    }

    /**
     * Processes damage values. The dmgType indicates the special ability for which this is (LRM, REAR, etc.).
     * location indicates the target of this damage, i.e. if it is the unit's standard damage and
     * standard ability block, the REAR ability or the TUR block, see ASLocationMapper.
     * When the location is rearLocation, dmgType must be REAR (as REAR has no LRM damage or the like)
     * When the location is turretLocation, TUR indicates it's the TUR's standard damage, LRM
     * indicates it's the LRM damage within the TUR block.
     */
    protected void processArcSpecial(ASArcs arc, BattleForceSUA dmgType) {
        report.startTentativeSection();
        report.addEmptyLine();
        report.addLine("--- " + arc + " " + dmgType + " Damage:", "");
        double[] damage = assembleSpecialDamage(dmgType, arc.toInt());

        String finalText = "Final value:";
        if (needsHeatAdjustment) {
            finalText = "Adjusted final value:";
            damage[0] *= heatAdjustFactorSM;
            damage[1] *= heatAdjustFactorSM;
            damage[2] *= heatAdjustFactorSM;
            damage[3] *= heatAdjustFactorSM;
        }

        if (qualifiesForSpecial(damage, dmgType)) {
            List<Double> damageList = Arrays.asList(damage[0], damage[1], damage[2], damage[3]);
            ASDamageVector finalDamage;
            if (dmgType.isAnyOf(STD, MSL, CAP, SCAP)) {
                finalDamage = ASDamageVector.createUpRndDmgMinus(damageList, rangesForSpecial(dmgType));
            } else {
                finalDamage = ASDamageVector.createNormRndDmg(damageList, rangesForSpecial(dmgType));
            }
            report.addLine(finalText,
                    formatAsVector(damage[0], damage[1], damage[2], damage[3], dmgType) + ", " + rdUp,
                    "" + dmgType + finalDamage);
            locations[arc.toInt()].getSpecials().addSPA(dmgType, finalDamage);
            report.endTentativeSection();
        } else {
            report.discardTentativeSection();
        }
    }

    @Override
    protected void reportAssignToLocations(Mounted weapon, BattleForceSUA sua, String abilityValue, int loc) {
        String locationText = " (" + ASLocationMapper.locationName(entity, loc) + ")";
        report.addLine(getWeaponDesc(weapon), sua.toString() + abilityValue + locationText);
    }

    @Override
    protected double[] assembleSpecialDamage(BattleForceSUA dmgType, int location) {
        double[] rawDmg = new double[4];
        Arrays.fill(rawDmg, 0);
        for (Mounted weapon : collectedWeapons.keySet()) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            double locationMultiplier = ASLocationMapper.damageLocationMultiplier(entity, location, weapon);
            if (!countsforSpecial(weapon, dmgType) || (locationMultiplier == 0)) {
                continue;
            }
            double dmgS = weaponType.getBattleForceDamage(SHORT_RANGE, weapon.getLinkedBy());
            double dmgM = weaponType.getBattleForceDamage(MEDIUM_RANGE, weapon.getLinkedBy());
            double dmgL = weaponType.getBattleForceDamage(LONG_RANGE, weapon.getLinkedBy());
            double dmgE = weaponType.getBattleForceDamage(EXTREME_RANGE, weapon.getLinkedBy());
            if ((dmgS > 0) || (dmgM > 0) || (dmgL > 0) || (dmgE > 0)) {
                double dmgMultiplier = getDamageMultiplier(weapon, weaponType);
                int weaponCount = collectedWeapons.getOrDefault(weapon, 1);
                String calculation = "+ " + weaponCount + " x ";
                calculation += formatAsVector(dmgS, dmgM, dmgL, dmgE, dmgType);
                calculation += (dmgMultiplier != 1) ? " x " + formatForReport(dmgMultiplier) : "";
                calculation += (locationMultiplier != 1) ? " x " + formatForReport(locationMultiplier) : "";
                rawDmg[0] += dmgS * dmgMultiplier * weaponCount * locationMultiplier;
                rawDmg[1] += dmgM * dmgMultiplier * weaponCount * locationMultiplier;
                rawDmg[2] += dmgL * dmgMultiplier * weaponCount * locationMultiplier;
                rawDmg[3] += dmgE * dmgMultiplier * weaponCount * locationMultiplier;
                report.addLine(weaponCount + " x " + getWeaponDesc(weapon), calculation,
                        "= " + formatAsVector(rawDmg[0], rawDmg[1], rawDmg[2], rawDmg[3], dmgType));
            }
        }
        return rawDmg;
    }

    @Override
    protected boolean countsforSpecial(Mounted weapon, BattleForceSUA dmgType) {
        WeaponType weaponType = (WeaponType) weapon.getType();
        switch (dmgType) {
            case MSL:
                return weaponType.getBattleForceClass() == WeaponType.BFCLASS_CAPITAL_MISSILE;
            case CAP:
                return weaponType.getBattleForceClass() == WeaponType.BFCLASS_CAPITAL;
            case SCAP:
                return weaponType.getBattleForceClass() == WeaponType.BFCLASS_SUBCAPITAL;
            case STD:
                return (weaponType.getBattleForceClass() != WeaponType.BFCLASS_CAPITAL_MISSILE)
                        && (weaponType.getBattleForceClass() != WeaponType.BFCLASS_CAPITAL)
                        && (weaponType.getBattleForceClass() != WeaponType.BFCLASS_SUBCAPITAL);
            case FLK:
                return weaponType.getBattleForceClass() == WeaponType.BFCLASS_FLAK;
            default:
                return false;
        }
    }

    protected void assembleAmmoCounts() {
    }

//    @Override
//    protected void assignSpecialAbilities(Mounted weapon, WeaponType weaponType) {
//        if (weaponType.getAmmoType() == AmmoType.T_INARC) {
//            report.addLine(weapon.getName(), "INARC1");
//            assignToLocation(weapon, INARC, 1);
//        } else if (weaponType.getAmmoType() == AmmoType.T_NARC) {
//            if (weaponType.hasFlag(WeaponType.F_BA_WEAPON)) {
//                report.addLine(weapon.getName(), "CNARC1");
//                assignToLocation(weapon, CNARC, 1);
//            } else {
//                report.addLine(weapon.getName(), "SNARC1");
//                assignToLocation(weapon, SNARC, 1);
//            }
//        }

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


//    }

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


    /**
     * Adjusts all damage values for overheat, if applicable (i.e., if the unit tracks heat,
     * if its heat output is sufficiently over its heat dissipation and for L/E values, if
     * the prerequisites for OVL are fulfilled.
     * Also assigns OVL where applicable.
     */
//    private void adjustForHeat() {
//            if (!entity.tracksHeat() && !element.usesArcs()) {
//                return;
//            }
//            int totalFrontHeat = getHeatGeneration(entity, element, false, false);
//            int heatCapacity = getHeatCapacity(entity, element);
//            report.addEmptyLine();
//            report.addSubHeader("Heat Adjustment:");
//            report.addLine("Heat Capacity: ", "", heatCapacity);
//            report.addLine("Total Heat M: ", "", totalFrontHeat);
//            if (totalFrontHeat - 4 <= heatCapacity) {
//                report.addLine("", "No adjustment needed", "");
//                return;
//            }
//
//            // Determine OV from the medium range damage
//            //TODO: this should not be necessary:
//            while (locations[0].standardDamage.size() < 4) {
//                weaponLocations[0].standardDamage.add(0.0);
//            }
//            double nonRounded = weaponLocations[0].standardDamage.get(RANGE_BAND_MEDIUM);
//            element.setOverheat(Math.min(heatDelta(nonRounded, heatCapacity, totalFrontHeat), 4));
//
//            report.addLine("Total Raw Dmg M: ", "", fmtDouble(nonRounded));
//            report.addLine("Total Adjusted Dmg M: ",
//                    fmtDouble(nonRounded) + " x " + heatCapacity + " / (" + totalFrontHeat + " - 4)",
//                    fmtDouble(nonRounded * heatCapacity / (totalFrontHeat - 4)));
//            report.addLine("Resulting Overheat", "OV " + element.getOverheat());
//
//            // Determine OVL from long range damage and heat
//            if (element.getOverheat() > 0 && element.usesOV()) {
//                double heatLong = getHeatGeneration(entity, element, false, true);
//                report.addLine("Total Heat L:", "", heatLong);
//                if (heatLong - 4 > heatCapacity) {
//                    double nonRoundedL = weaponLocations[0].standardDamage.get(RANGE_BAND_LONG);
//                    report.addLine("Total Damage L before Adjustment: ", "", nonRoundedL);
//                    if (heatDelta(nonRoundedL, heatCapacity, heatLong) >= 1) {
//                        report.addLine("Resulting SPA", "OVL", "");
//                        element.addSPA(OVL);
//                    }
//                }
//            }
//
//            // Adjust all weapon damages (E for units with OVL and arced units, M otherwise)
//            int maxAdjustmentRange = 1 + ((element.hasSUA(OVL) || element.usesArcs()) ? RANGE_BAND_EXTREME : RANGE_BAND_MEDIUM);
//            double frontadjustment = (double) heatCapacity / (totalFrontHeat - 4);
//            double rearHeat = getHeatGeneration(entity, element, true, false);
//            double rearAdjustment = rearHeat - 4 > heatCapacity ? heatCapacity / (rearHeat - 4) : 1;
//            // No special treatment for the rear arc of Large Aero units
//            if (element.usesArcs()) {
//                rearAdjustment = frontadjustment;
//                if (element.getFrontArc().hasSPA(PNT)) {
//                    double pntValue = (Double) element.getFrontArc().getSPA(PNT);
//                    element.getFrontArc().getSpecials().replaceSPA(PNT, pntValue / frontadjustment);
//                }
//                if (element.getLeftArc().hasSPA(PNT)) {
//                    double pntValue = (Double) element.getLeftArc().getSPA(PNT);
//                    element.getLeftArc().getSpecials().replaceSPA(PNT, pntValue / frontadjustment);
//                }
//                if (element.getRightArc().hasSPA(PNT)) {
//                    double pntValue = (Double) element.getRightArc().getSPA(PNT);
//                    element.getRightArc().getSpecials().replaceSPA(PNT, pntValue / frontadjustment);
//                }
//                if (element.getRearArc().hasSPA(PNT)) {
//                    double pntValue = (Double) element.getRearArc().getSPA(PNT);
//                    element.getRearArc().getSpecials().replaceSPA(PNT, pntValue / frontadjustment);
//                }
//            }
//            for (int loc = 0; loc < weaponLocations.length; loc++) {
//                ASConverter.WeaponLocation wloc = weaponLocations[loc];
//                double adjustment = locationNames[loc].equals("REAR") ? rearAdjustment : frontadjustment;
//                for (int i = 0; i < Math.min(maxAdjustmentRange, wloc.standardDamage.size()); i++) {
//                    wloc.standardDamage.set(i, heatAdjust(wloc.standardDamage.get(i), adjustment));
//                }
//                for (List<Double> damage : wloc.specialDamage.values()) {
//                    for (int i = 0; i < Math.min(maxAdjustmentRange, damage.size()); i++) {
//                        damage.set(i, heatAdjust(damage.get(i), adjustment));
//                    }
//                }
//                // IF is long-range fire; only adjust when the unit can overheat even in long range
//                if (element.hasSUA(OVL)) {
//                    //TODO: really adjust this with round up to tenth?
//                    wloc.indirect = heatAdjust(wloc.indirect, adjustment);
//                }
//            }
//    }
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

}


