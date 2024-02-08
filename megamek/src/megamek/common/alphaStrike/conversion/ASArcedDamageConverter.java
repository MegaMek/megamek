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

    private final Map<Mounted, Integer> collectedWeapons = new HashMap<>();

    protected ASArcedDamageConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
        locations = new ASArcSummary[ASLocationMapper.damageLocationsCount(entity)];
        for (int index = 0; index < locationNames.length; index++) {
            locations[index] = new ASArcSummary();
        }
        // Flatten the weaponlist as weapon bays are not relevant for AS conversion
        List<Mounted> flattenedWeaponList = new ArrayList<>();
        for (Mounted<?> weapon : entity.getWeaponList()) {
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

    @Override
    protected void calculateHeatAdjustment() {
        report.addLine("--- Heat Adjustment", "", "");
        int heatCapacity = getHeatCapacity();
        int totalHeat = getHeatGeneration(false, false);
        report.addLine("Heat Capacity", heatCapacity + "");
        report.addLine("Heat Generation", totalHeat + "");
        if (totalHeat - 4 > heatCapacity) {
            needsHeatAdjustment = true;
            heatAdjustFactor = (double) heatCapacity / (totalHeat - 4);
            report.addLine("Adjustment factor", heatCapacity + " / (" + totalHeat + " - 4)",
                    "= " + formatForReport(heatAdjustFactor));
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
        processArcSpecial(arc, STD);
        processArcSpecial(arc, MSL);
        processArcSpecial(arc, CAP);
        processArcSpecial(arc, SCAP);
        processArcSpecial(arc, FLK);
        processArcSpecial(arc, PNT);
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
        double[] damage = assembleSpecialDamage(dmgType, ASConverter.toInt(arc));

        String finalText = "Final value:";
        if (needsHeatAdjustment) {
            finalText = "Adjusted final value:";
            damage[0] *= heatAdjustFactor;
            damage[1] *= heatAdjustFactor;
            damage[2] *= heatAdjustFactor;
            damage[3] *= heatAdjustFactor;
        }

        if (qualifiesForSpecial(damage, dmgType)) {
            List<Double> damageList = Arrays.asList(damage[0], damage[1], damage[2], damage[3]);
            if (dmgType == PNT) {
                int finalPNTDamage = ASConverter.roundUp(roundUpToTenth(damage[0]));
                report.addLine(finalText, formatForReport(damage[0]) + ", " + rdUp, "PNT" + finalPNTDamage);
                locations[ASConverter.toInt(arc)].mergeSUA(PNT, finalPNTDamage);
            } else {
                ASDamageVector finalDamage;
                if (dmgType.isAnyOf(STD, MSL, CAP, SCAP)) {
                    finalDamage = ASDamageVector.createUpRndDmgMinus(damageList, rangesForSpecial(dmgType));
                } else {
                    finalDamage = ASDamageVector.createNormRndDmg(damageList, rangesForSpecial(dmgType));
                }
                report.addLine(finalText,
                        formatAsVector(damage[0], damage[1], damage[2], damage[3], dmgType) + ", " + rdUp,
                        "" + dmgType + finalDamage);
                locations[ASConverter.toInt(arc)].setSUA(dmgType, finalDamage);
            }
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
            double dmgS = determineSpecialsDamage(weaponType, weapon.getLinkedBy(), SHORT_RANGE, dmgType);
            double dmgM = determineSpecialsDamage(weaponType, weapon.getLinkedBy(), MEDIUM_RANGE, dmgType);
            double dmgL = determineSpecialsDamage(weaponType, weapon.getLinkedBy(), LONG_RANGE, dmgType);
            double dmgE = determineSpecialsDamage(weaponType, weapon.getLinkedBy(), EXTREME_RANGE, dmgType);
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
    protected void assembleAmmoCounts() { }

    @Override
    protected void writeLocationsToElement() {
        for (ASArcs arc : ASArcs.values()) {
            element.setArc(arc, (ASArcSummary) locations[ASConverter.toInt(arc)]);
        }
    }
}