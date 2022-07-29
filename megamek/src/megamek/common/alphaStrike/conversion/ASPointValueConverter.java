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
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static megamek.client.ui.swing.calculationReport.CalculationReport.fmt;
import static megamek.common.alphaStrike.ASUnitType.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASPointValueConverter {

    protected final AlphaStrikeElement element;
    protected final CalculationReport report;

    protected double offensiveValue = 0;
    protected double defensiveValue = 0;
    protected double dir = 0;
    protected double subTotal = 0;

    static ASPointValueConverter getPointValueConverter(AlphaStrikeElement element, CalculationReport report) {
        if (element.isLargeAerospace()) {
            return new ASLargeAeroPointValueConverter(element, report);
        } else if (element.isType(AF, CF) || element.isAerospaceSV()) {
            return new ASAeroPointValueConverter(element, report);
        } else {
            return new ASPointValueConverter(element, report);
        }
    }

    protected ASPointValueConverter(AlphaStrikeElement element, CalculationReport report) {
        this.element = element;
        this.report = report;
    }

    protected int getBasePointValue() {
        report.addEmptyLine();
        report.addSubHeader("Point Value:");
        report.addLine("--- Offensive Value:", "");

        // Offensive Value
        processStandardDamageAndOV();
        processSize();
        processOffensiveSPAMods();
        processOffensiveBlanketMod();

        // Defensive Value
        report.addEmptyLine();
        report.addLine("--- Defensive Value:", "");
        processMovement();
        processDefensiveSPAMods();
        processDefensiveFactors();
        report.addLine("Defensive Value:",
                fmt(defensiveValue) + " + " + fmt(dir),
                "= " + fmt(defensiveValue + dir));
        defensiveValue += dir;

        subTotal = offensiveValue + defensiveValue;
        report.addEmptyLine();
        report.addLine("Subtotal", fmt(offensiveValue) + " + " + fmt(defensiveValue), fmt(subTotal));

        processGroundCapabilities();
        processForceBonus();

        int pointValue = Math.max(1, (int) Math.round(subTotal));
        report.addEmptyLine();
        report.addResultLine("Base Point Value", "round normal", "" + pointValue);
        return pointValue;
    }

    protected void processStandardDamageAndOV() {
        double dmgS = getPointValueSDamage(element);
        double dmgM = getPointValueMDamage(element);
        double dmgL = getPointValueLDamage(element);
        offensiveValue = dmgS + dmgM * 2 + dmgL;
        report.addLine("Damage", fmt(dmgS) + " + 2 x " + fmt(dmgM) + " + " + fmt(dmgL), fmt(offensiveValue));

        if (element.getOverheat() >= 1) {
            double overheatFactor = 1 + 0.5 * (element.getOverheat() - 1);
            overheatFactor /= (dmgM + dmgL == 0) ? 2 : 1;
            offensiveValue += overheatFactor;
            report.addLine("Overheat", "+ " + fmt(overheatFactor), "= " + fmt(offensiveValue));
        }
    }

    protected void processSize() {
        if (element.isType(BM, PM)) {
            offensiveValue += 0.5 * element.getSize();
            report.addLine("Size", "+ " + element.getSize() + " / 2", "= " + fmt(offensiveValue));
        }
    }

    protected void processMovement() {
        defensiveValue += 0.125 * getHighestMove(element);
        report.addLine("Movement", getHighestMove(element) + " / 8", "= " + fmt(defensiveValue));
        if (element.hasMovementMode("a")) {
            if (element.getMovement("a") >= 10) {
                defensiveValue += 1;
                report.addLine("High Thrust", "", "+ 1");
            }
            defensiveValue += 0.25 * element.getMovement("a");
            report.addLine("Aero Movement",
                    element.getMovement("a") + " / 4",
                    "+ " + 0.25 * element.getMovement("a"));
        }

        if (element.isJumpCapable()) {
            defensiveValue += 0.5;
            report.addLine("Jump-capable", "+ 0.5", "= " + fmt(defensiveValue));
        }
    }

    int getSkillAdjustedPointValue() {
        int basePointValue = getBasePointValue();
        if (element.getSkill() == 4) {
            return basePointValue;
        }

        int multiplier = 1;
        int newPointValue = basePointValue;
        if (element.getSkill() > 4) {
            if (basePointValue > 14) {
                multiplier += (basePointValue - 5) / 10;
            }
            newPointValue = basePointValue - (element.getSkill() - 4) * multiplier;
        } else if (element.getSkill() < 4) {
            if (basePointValue > 7) {
                multiplier += (basePointValue - 3) / 5;
            }
            newPointValue = basePointValue + (4 - element.getSkill()) * multiplier;
        }
        newPointValue = Math.max(newPointValue, 1);
        report.addLine("Skill-adjusted Point Value", "", "" + newPointValue);
        return newPointValue;
    }

    /** Returns the Ground Offensive SPA modifier, ASC p.139. */
    protected void processOffensiveSPAMods() {
        processOffensiveSPAMod(TAG, e -> 0.5);
        processOffensiveSPAMod(LTAG, e -> 0.25);
        processOffensiveSPAMod(SNARC, e -> (double) (int) element.getSUA(SNARC));
        processOffensiveSPAMod(INARC, e -> (double) (int) element.getSUA(INARC));
        processOffensiveSPAMod(CNARC, e -> 0.5 * (int) element.getSUA(CNARC));
        processOffensiveSPAMod(TSM, e -> 1.0);
        processOffensiveSPAMod(ECS, e -> 0.25);
        processOffensiveSPAMod(MEL, e -> 0.5);
        processOffensiveSPAMod(MDS, e -> (double) (int) element.getSUA(MDS));
        processOffensiveSPAMod(MTAS, e -> (double) (int) element.getSUA(MTAS));
        processOffensiveSPAMod(BTAS, e -> 0.25 * (int) element.getSUA(BTAS));
        processOffensiveSPAMod(TSEMP, e -> 5 * (double) (int) element.getSUA(TSEMP));
        processOffensiveSPAMod(TSEMPO, e -> Math.min(5.0, (int) element.getSUA(TSEMPO)));
        processOffensiveSPAMod(BT, e -> 0.5 * getHighestMove(element) * element.getSize());
        processOffensiveSPAMod(IATM, e -> (double) ((ASDamageVector) element.getSUA(IATM)).L.damage);
        processOffensiveSPAMod(OVL, e -> 0.25 * element.getOverheat());
        processOffensiveSPAMod(HT, e -> {
            ASDamageVector ht = (ASDamageVector) element.getSUA(HT);
            return Math.max(ht.S.damage, Math.max(ht.M.damage, ht.L.damage)) + ((ht.M.damage > 0) ? 0.5 : 0);
        });
        processOffensiveSPAMod(IF,
                e -> (element.getIF().minimal ? 0.5 : element.getIF().damage));
        if (element.hasSUA(RHS)) {
            if (element.hasSUA(OVL)) {
                offensiveValue += 1;
                report.addLine("Offensive SPA", "+ 1 (RHS and OVL)", "");
            } else if (element.getOverheat() > 0) {
                offensiveValue += 0.5;
                report.addLine("Offensive SPA", "+ 0.5 (RHS and OV > 0)", "");
            } else {
                offensiveValue += 0.25;
                report.addLine("Offensive SPA", "+ 0.25 (RHS)", "");
            }
        }
        processArtyOffensiveSPAMods();
    }

    /** Returns the Artillery part of the Ground Offensive SPA modifier, ASC p.139. */
    protected void processArtyOffensiveSPAMods() {
        processOffensiveSPAMod(ARTAIS, e -> 12.0 * (int) e.getSUA(ARTAIS));
        processOffensiveSPAMod(ARTAC, e -> 12.0 * (int) e.getSUA(ARTAC));
        processOffensiveSPAMod(ARTT, e -> 6.0 * (int) e.getSUA(ARTT));
        processOffensiveSPAMod(ARTS, e -> 12.0 * (int) e.getSUA(ARTS));
        processOffensiveSPAMod(ARTBA, e -> 6.0 * (int) e.getSUA(ARTBA));
        processOffensiveSPAMod(ARTLTC, e -> 12.0 * (int) e.getSUA(ARTLTC));
        processOffensiveSPAMod(ARTSC, e -> 6.0 * (int) e.getSUA(ARTSC));
        processOffensiveSPAMod(ARTCM5, e -> 30.0 * (int) e.getSUA(ARTCM5));
        processOffensiveSPAMod(ARTCM7, e -> 54.0 * (int) e.getSUA(ARTCM7));
        processOffensiveSPAMod(ARTCM9, e -> 72.0 * (int) e.getSUA(ARTCM9));
        processOffensiveSPAMod(ARTCM12, e -> 93.0 * (int) e.getSUA(ARTCM12));
        processOffensiveSPAMod(ARTLT, e -> 27.0 * (int) e.getSUA(ARTLT));
        processOffensiveSPAMod(ARTTC, e -> 3.0 * (int) e.getSUA(ARTTC));
    }

    /**
     * A helper method for SPA modifiers of various types. Returns the modifier (or 0)
     * and adds a line to the report.
     *
     * @param spa The SPA to be checked
     * @param spaMod A Function object that returns the modifier for the element and SPA. This is
     */
    protected void processOffensiveSPAMod(BattleForceSUA spa, Function<AlphaStrikeElement, Double> spaMod) {
        if (element.hasSUA(spa)) {
            double modifier = spaMod.apply(element);
            String spaString = element.getSpecialAbilities().formatSPAString(spa, element.getSUA(spa));
            offensiveValue += modifier;
            report.addLine("Offensive SPA", "+ " + modifier + " (" + spaString + ")", "= " + fmt(offensiveValue));
        }
    }

    /**
     * A helper method for SPA modifiers of various types. Returns the modifier (or 0)
     * and adds a line to the report.
     *
     * @param spa The SPA to be checked
     * @param spaMod A Function object that returns the modifier for the element and SPA. This is
     */
    protected void processDefensiveSPAMod(BattleForceSUA spa, Function<AlphaStrikeElement, Double> spaMod) {
        if (element.hasSUA(spa)) {
            double modifier = spaMod.apply(element);
            String spaString = element.getSpecialAbilities().formatSPAString(spa, element.getSUA(spa));
            defensiveValue += modifier;
            report.addLine("Defensive SPA", "+ " + modifier + " (" + spaString + ")", "= " + fmt(defensiveValue));
        }
    }

    protected void processOffensiveBlanketMod() {
        double blanketMod = 1;
        List<String> modifierList = new ArrayList<>();
        if (element.hasSUA(ATAC)) {
            blanketMod += 0.1;
            modifierList.add("ATAC");
        }
        if (element.hasSUA(VRT)) {
            blanketMod += 0.1;
            modifierList.add("VRT");
        }
        if (element.hasSUA(SHLD)) {
            blanketMod -= 0.1;
            modifierList.add("SHLD");
        }
        if (element.isType(SV, IM)) {
            if (!element.hasAnySUAOf(AFC, BFC)) {
                blanketMod -= 0.2;
                modifierList.add("No FC");
            }
        } else if (element.hasSUA(BFC)) {
            blanketMod -= 0.1;
            modifierList.add("BFC");
        }
        String modifiers = modifierList.isEmpty() ? "" : " (" + String.join(", ", modifierList) + ")";
        report.addLine("Offensive Value:",
                fmt(offensiveValue) + " x " + fmt(blanketMod) + modifiers,
                "= " + fmt(offensiveValue * blanketMod));
        offensiveValue *= blanketMod;
    }

    protected void processDefensiveSPAMods() {
        double armorThird = Math.floor((double)element.getFullArmor() / 3);
        double barFactor = element.hasSUA(BAR) ? 0.5 : 1;

        processDefensiveSPAMod(ABA, e -> 0.5);
        processDefensiveSPAMod(AMS, e -> 1.0);
        processDefensiveSPAMod(FR, e -> 0.5);
        processDefensiveSPAMod(RAMS, e -> 1.25);
        processDefensiveSPAMod(BHJ2, e -> barFactor * armorThird);
        processDefensiveSPAMod(RCA, e -> barFactor * armorThird);
        processDefensiveSPAMod(SHLD, e -> barFactor * armorThird);
        processDefensiveSPAMod(BHJ3, e -> barFactor * 1.5 * armorThird);
        processDefensiveSPAMod(BRA, e -> barFactor * 0.75 * armorThird);
        processDefensiveSPAMod(IRA, e -> barFactor * 0.5 * armorThird);
        if (element.hasSUA(CR) && (element.getFullStructure() >= 3)) {
            defensiveValue += 0.25;
            report.addLine("Defensive SPA", "+ 0.25 (CR)", "= " + fmt(defensiveValue));
        }
        if (element.hasSUA(ARM) && (element.getFullStructure() > 1)) {
            defensiveValue += 0.5;
            report.addLine("Defensive SPA", "+ 0.5 (ARM)", "= " + fmt(defensiveValue));
        }
    }

    protected void processDefensiveFactors() {
        report.addLine("- DIR:", "");
        processArmor();
        processStructure();
        double defFactor = getDefenseFactor();
        report.addLine("- DIR:",
                fmt(dir) + " x " + fmt(defFactor) + ", round to half",
                "= " + fmt(0.5 * Math.round(dir * defFactor * 2)));
        dir = 0.5 * Math.round(dir * defFactor * 2);
    }

    private void processArmor() {
        double armorMultiplier = 2;
        List<String> modifierList = new ArrayList<>();
        if (element.isType(CV)) {
            modifierList.add("MV " + element.getPrimaryMovementType());
            if (element.getMovementModes().contains("t") || element.getMovementModes().contains("n")) {
                armorMultiplier = 1.8;
            } else if (element.getMovementModes().contains("h") || element.getMovementModes().contains("w")) {
                armorMultiplier = 1.7;
            } else if (element.getMovementModes().contains("v") || element.getMovementModes().contains("g")) {
                armorMultiplier = 1.5;
            }
            if (element.hasSUA(ARS)) {
                armorMultiplier += 0.1;
                modifierList.add("ARS");
            }
        }
        if (element.hasSUA(BAR)) {
            armorMultiplier /= 2;
            modifierList.add("BAR");
        }
        dir = element.getFullArmor() * armorMultiplier;
        String modifiers = modifierList.isEmpty() ? "" : " (" + String.join(", ", modifierList) + ")";
        report.addLine("- Armor",
                element.getFullArmor() + " x " + fmt(armorMultiplier) + modifiers,
                "= " + fmt(dir));
    }

    private void processStructure() {
        double strucMultiplier = 1;
        String modifiers = "";
        if (element.isInfantry()) {
            modifiers = " (Infantry)";
            strucMultiplier = 2;
        } else  if (element.isType(IM) || element.hasSUA(BAR)) {
            modifiers = element.isType(IM) ? " (IM)" : "(BAR)";
            strucMultiplier = 0.5;
        }
        dir += element.getFullStructure() * strucMultiplier;
        report.addLine("- Structure",
                "+ " + element.getFullStructure() + " x " + fmt(strucMultiplier) + modifiers,
                "= " + fmt(dir));
    }

    private double getDefenseFactor() {
        double result = 0;
        double movemod = element.getTMM();
        List<String> modifierList = new ArrayList<>();
        if (element.hasSUA(MAS) && (3 > movemod)) {
            result += 3;
            modifierList.add("MAS");
        } else if (element.hasSUA(LMAS) && (2 > movemod)) {
            result += 2;
            modifierList.add("LMAS");
        } else {
            result += movemod;
            modifierList.add("TMM");
        }
        if (element.isType(BA, PM)) {
            result += 1;
            modifierList.add(element.isBattleArmor() ? "BA" : "PM");
        }
        if ((element.isType(CV)) && (element.getMovementModes().contains("g")
                || element.getMovementModes().contains("v"))) {
            result++;
            modifierList.add("VTOL/WiGE");
        }
        if (element.hasSUA(STL)) {
            result += 2;
            modifierList.add("STL");
        }
        if (element.hasAnySUAOf(LG, SLG, VLG)) {
            result -= 1;
            modifierList.add("LG/SLG/VLG");
        }
        if (element.isJumpCapable() && (element.isInfantry() || !element.getStandardDamage().hasDamage())) {
            result += 1;
            modifierList.add("Jump");
        }
        double multiplier = (result <= 2 ? 0.1 : 0.25);
        double defFactor = 1 + multiplier * result;
        String modifiers = " (" + String.join(", ", modifierList) + ")";
        report.addLine("- Defense Factor",
                "1 + " + fmt(multiplier) + " x " + fmt(result) + modifiers,
                "");
        return defFactor;
    }

    protected void processGroundCapabilities() {
        double bonus = agileBonus();
        bonus += c3Bonus();
        bonus -= brawlerMalus();
        subTotal += bonus;
    }

    /**
     * Determines the Brawler Malus, AlphaStrike Companion Errata v1.4, p.17
     * This is 0 if not applicable.
     */
    private double brawlerMalus() {
        int move = getHighestMove(element);
        double multiplier = 0;
        if (move >= 2 && !element.hasAnySUAOf(BT, ARTS, C3BSM, C3BSS, C3EM,
                C3I, C3M, C3S, AC3, NC3, NOVA, C3RS, ECM, AECM, ARTAC, ARTAIS, ARTBA, ARTCM12, ARTCM5, ARTCM7,
                ARTCM9, ARTLT, ARTLTC, ARTSC, ARTT, ARTTC)) {
            double dmgS = getPointValueSDamage(element);
            double dmgM = getPointValueMDamage(element);
            double dmgL = getPointValueLDamage(element);

            boolean onlyShortRange = (dmgM + dmgL) == 0 && (dmgS > 0);
            boolean onlyShortMediumRange = (dmgL == 0) && (dmgS + dmgM > 0);
            if ((move >= 6) && (move <= 10) && onlyShortRange) {
                multiplier = 0.25;
            } else if ((move < 6) && onlyShortRange) {
                multiplier = 0.5;
            } else if ((move < 6) && onlyShortMediumRange) {
                multiplier = 0.25;
            }
        }
        double malus = roundToHalf(subTotal * multiplier);
        if (multiplier != 0) {
            report.addLine("Brawler", "- " + malus, "");
        }
        return roundToHalf(multiplier * subTotal);
    }

    /** Determines the Agile Bonus, AlphaStrike Companion Errata v1.4, p.17  */
    private double agileBonus() {
        double result = 0;
        if (element.getTMM() >= 2) {
            double dmgS = element.getStandardDamage().S.minimal ? 0.5 : element.getStandardDamage().S.damage;
            double dmgM = element.getStandardDamage().M.minimal ? 0.5 : element.getStandardDamage().M.damage;
            if (dmgM > 0) {
                result = (element.getTMM() - 1) * dmgM;
            } else if (element.getTMM() >= 3) {
                result = (element.getTMM() - 2) * dmgS;
            }
        }
        if (result != 0) {
            report.addLine("Agile", "+ " + result, "");
        }
        return roundToHalf(result);
    }

    /** C3 Bonus, AlphaStrike Companion Errata v1.4, p.17 */
    private double c3Bonus() {
        if (element.hasAnySUAOf(C3BSM, C3BSS, C3EM, C3I, C3M, C3S, AC3, NC3, NOVA)) {
            double bonus = roundToHalf(subTotal * 0.05);
            report.addLine("C3 Bonus", "+ " + bonus, "");
            return roundToHalf(subTotal * 0.05);
        } else {
            return 0;
        }
    }

    /** Adds the force bonus to the current subTotal and returns the new subTotal. */
    protected void processForceBonus() {
        String calculation = "";
        if (element.hasSUA(AECM)) {
            subTotal += 3;
            calculation += "+ 3 (AECM) ";
        }
        if (element.hasSUA(BH)) {
            subTotal += 2;
            calculation += "+ 2 (BH) ";
        }
        if (element.hasSUA(C3RS)) {
            subTotal += 2;
            calculation += "+ 2 (C3RS) ";
        }
        if (element.hasSUA(ECM)) {
            subTotal += 2;
            calculation += "+ 2 (ECM) ";
        }
        if (element.hasSUA(RCN)) {
            subTotal += 2;
            calculation += "+ 2 (RCN) ";
        }
        if (element.hasSUA(TRN)) {
            subTotal += 2;
            calculation += "+ 2 (TRN) ";
        }
        if (element.hasSUA(LPRB)) {
            subTotal += 1;
            calculation += "+ 1 (LPRB) ";
        }
        if (element.hasSUA(PRB)) {
            subTotal += 1;
            calculation += "+ 1 (PRB) ";
        }
        if (element.hasSUA(LECM)) {
            subTotal += 0.5;
            calculation += "+ 0.5 (LECM) ";
        }
        if (element.hasSUA(MHQ)) {
            int mhqValue = (int) element.getSUA(MHQ);
            subTotal += mhqValue;
            calculation += "+ " + mhqValue + " (MHQ) ";
        }
        if (!calculation.isBlank()) {
            report.addLine("Force Bonus", calculation, "= " + fmt(subTotal));
        }
    }

    /** @return The Short Range damage value with minimal damage represented as 0.5. */
    private static double getPointValueSDamage(AlphaStrikeElement element) {
        return element.getStandardDamage().S.minimal ? 0.5 : element.getStandardDamage().S.damage;
    }

    /** @return The Medium Range damage value with minimal damage represented as 0.5. */
    private static double getPointValueMDamage(AlphaStrikeElement element) {
        return element.getStandardDamage().M.minimal ? 0.5 : element.getStandardDamage().M.damage;
    }

    /** @return The Long Range damage value with minimal damage represented as 0.5. */
    private static double getPointValueLDamage(AlphaStrikeElement element) {
        return element.getStandardDamage().L.minimal ? 0.5 : element.getStandardDamage().L.damage;
    }

    /** @return The highest movement capability of any of the element's movement modes. */
    protected int getHighestMove(AlphaStrikeElement element) {
        return element.getMovement().values().stream().mapToInt(m -> m).max().orElse(0);
    }

    double roundToHalf(double number) {
        return 0.5 * Math.round(number * 2);
    }
}