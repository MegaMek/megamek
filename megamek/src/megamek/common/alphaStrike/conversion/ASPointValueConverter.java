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
import megamek.common.alphaStrike.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
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
        processOffensiveSUAMods();
        processOffensiveBlanketMod();

        // Defensive Value
        report.addEmptyLine();
        report.addLine("--- Defensive Value:", "");
        processMovement();
        processDefensiveSUAMods();
        processDefensiveFactors();
        report.addLine("Defensive Value:",
                formatForReport(defensiveValue) + " + " + formatForReport(dir),
                "= " + formatForReport(defensiveValue + dir));
        defensiveValue += dir;

        subTotal = offensiveValue + defensiveValue;
        report.addEmptyLine();
        report.addLine("Subtotal", formatForReport(offensiveValue) + " + " + formatForReport(defensiveValue), formatForReport(subTotal));

        processGroundCapabilities();
        processForceBonus();

        int pointValue = Math.max(1, (int) Math.round(subTotal));
        report.addEmptyLine();
        report.addResultLine("Base Point Value", "round normal", "" + pointValue);
        return pointValue;
    }

    protected void processStandardDamageAndOV() {
        double dmgS = pointValueSDamage(element);
        double dmgM = pointValueMDamage(element);
        double dmgL = pointValueLDamage(element);
        offensiveValue = dmgS + dmgM * 2 + dmgL;
        report.addLine("Damage", formatForReport(dmgS) + " + 2 x " + formatForReport(dmgM) + " + " + formatForReport(dmgL), formatForReport(offensiveValue));

        if (element.getOV() >= 1) {
            double overheatFactor = 1 + 0.5 * (element.getOV() - 1);
            overheatFactor /= (dmgM + dmgL == 0) ? 2 : 1;
            offensiveValue += overheatFactor;
            report.addLine("Overheat", "+ " + formatForReport(overheatFactor), "= " + formatForReport(offensiveValue));
        }
    }

    protected void processSize() {
        if (element.isMek() || element.isProtoMek()) {
            offensiveValue += 0.5 * element.getSize();
            report.addLine("Size", "+ " + element.getSize() + " / 2", "= " + formatForReport(offensiveValue));
        }
    }

    protected void processMovement() {
        defensiveValue += 0.125 * getHighestMove(element);
        report.addLine("Movement", getHighestMove(element) + " / 8", "= " + formatForReport(defensiveValue));
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
            report.addLine("Jump-capable", "+ 0.5", "= " + formatForReport(defensiveValue));
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

    /** Returns the Ground Offensive SUA modifier, ASC p.139. */
    protected void processOffensiveSUAMods() {
        processOffensiveSUAMod(TAG, e -> 0.5);
        processOffensiveSUAMod(LTAG, e -> 0.25);
        processOffensiveSUAMod(SNARC, e -> (double) (int) element.getSUA(SNARC));
        processOffensiveSUAMod(INARC, e -> (double) (int) element.getSUA(INARC));
        processOffensiveSUAMod(CNARC, e -> 0.5 * (int) element.getSUA(CNARC));
        processOffensiveSUAMod(TSM, e -> 1.0);
        processOffensiveSUAMod(ECS, e -> 0.25);
        processOffensiveSUAMod(MEL, e -> 0.5);
        processOffensiveSUAMod(MDS, e -> (double) (int) element.getSUA(MDS));
        processOffensiveSUAMod(MTAS, e -> (double) (int) element.getSUA(MTAS));
        processOffensiveSUAMod(BTAS, e -> 0.25 * (int) element.getSUA(BTAS));
        processOffensiveSUAMod(TSEMP, e -> 5 * (double) (int) element.getSUA(TSEMP));
        processOffensiveSUAMod(TSEMPO, e -> Math.min(5.0, (int) element.getSUA(TSEMPO)));
        processOffensiveBT();
        processOffensiveSUAMod(IATM, e -> (double) ((ASDamageVector) element.getSUA(IATM)).L.damage);
        processOffensiveSUAMod(OVL, e -> 0.25 * element.getOV());
        processOffensiveSUAMod(HT, e -> {
            ASDamageVector ht = element.getHT();
            return Math.max(ht.S.damage, Math.max(ht.M.damage, ht.L.damage)) + ((ht.M.damage > 0) ? 0.5 : 0);
        });
        processOffensiveSUAMod(IF,
                e -> (element.getIF().minimal ? 0.5 : element.getIF().damage));
        if (element.hasSUA(RHS)) {
            if (element.hasSUA(OVL)) {
                offensiveValue += 1;
                report.addLine("Offensive SUA", "+ 1 (RHS and OVL)", "");
            } else if (element.getOV() > 0) {
                offensiveValue += 0.5;
                report.addLine("Offensive SUA", "+ 0.5 (RHS and OV > 0)", "");
            } else {
                offensiveValue += 0.25;
                report.addLine("Offensive SUA", "+ 0.25 (RHS)", "");
            }
        }
        processArtyOffensiveSUAMods();
    }

    protected void processOffensiveBT() {
        processOffensiveSUAMod(BT, e -> 0.5 * getHighestMove(element) * element.getSize());
    }

    /** Returns the Artillery part of the Ground Offensive SUA modifier, ASC p.139. */
    protected void processArtyOffensiveSUAMods() {
        processOffensiveSUAMod(ARTAIS, e -> 12.0 * (int) e.getSUA(ARTAIS));
        processOffensiveSUAMod(ARTAC, e -> 12.0 * (int) e.getSUA(ARTAC));
        processOffensiveSUAMod(ARTT, e -> 6.0 * (int) e.getSUA(ARTT));
        processOffensiveSUAMod(ARTS, e -> 12.0 * (int) e.getSUA(ARTS));
        processOffensiveSUAMod(ARTBA, e -> 6.0 * (int) e.getSUA(ARTBA));
        processOffensiveSUAMod(ARTLTC, e -> 12.0 * (int) e.getSUA(ARTLTC));
        processOffensiveSUAMod(ARTSC, e -> 6.0 * (int) e.getSUA(ARTSC));
        processOffensiveSUAMod(ARTCM5, e -> 30.0 * (int) e.getSUA(ARTCM5));
        processOffensiveSUAMod(ARTCM7, e -> 54.0 * (int) e.getSUA(ARTCM7));
        processOffensiveSUAMod(ARTCM9, e -> 72.0 * (int) e.getSUA(ARTCM9));
        processOffensiveSUAMod(ARTCM12, e -> 93.0 * (int) e.getSUA(ARTCM12));
        processOffensiveSUAMod(ARTLT, e -> 27.0 * (int) e.getSUA(ARTLT));
        processOffensiveSUAMod(ARTTC, e -> 3.0 * (int) e.getSUA(ARTTC));
    }

    /**
     * A helper method for SUA modifiers of various types. Returns the modifier (or 0)
     * and adds a line to the report.
     *
     * @param sua The SUA to be checked
     * @param suaMod A Function object that returns the modifier for the element and SUA. This is
     */
    protected void processOffensiveSUAMod(BattleForceSUA sua, Function<AlphaStrikeElement, Double> suaMod) {
        if (element.hasSUA(sua)) {
            double modifier = suaMod.apply(element);
            String suaString = AlphaStrikeHelper.formatAbility(sua, element.getSpecialAbilities(), element, ", ");
            offensiveValue += modifier;
            report.addLine("Offensive SUA", "+ " + formatForReport(modifier) + " (" + suaString + ")", "= " + formatForReport(offensiveValue));
        }
    }

    /**
     * A helper method for SUA modifiers of various types. Returns the modifier (or 0)
     * and adds a line to the report.
     *
     * @param sua The SUA to be checked
     * @param suaMod A Function object that returns the modifier for the element and SUA. This is
     */
    protected void processDefensiveSUAMod(BattleForceSUA sua, Function<AlphaStrikeElement, Double> suaMod) {
        if (element.hasSUA(sua)) {
            double modifier = suaMod.apply(element);
            String suaString = AlphaStrikeHelper.formatAbility(sua, element.getSpecialAbilities(), element, ", ");
            defensiveValue += modifier;
            report.addLine("Defensive SUA", "+ " + formatForReport(modifier) + " (" + suaString + ")", "= " + formatForReport(defensiveValue));
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
        }
        if (element.hasSUA(BFC)) {
            blanketMod -= 0.1;
            modifierList.add("BFC");
        }
        String modifiers = modifierList.isEmpty() ? "" : " (" + String.join(", ", modifierList) + ")";
        report.addLine("Offensive Value:",
                formatForReport(offensiveValue) + " x " + formatForReport(blanketMod) + modifiers,
                "= " + formatForReport(offensiveValue * blanketMod));
        offensiveValue *= blanketMod;
    }

    protected void processDefensiveSUAMods() {
        double armorThird = Math.floor((double)element.getFullArmor() / 3);
        double barFactor = element.hasSUA(BAR) ? 0.5 : 1;

        processDefensiveSUAMod(ABA, e -> 0.5);
        processDefensiveSUAMod(AMS, e -> 1.0);
        processDefensiveSUAMod(FR, e -> 0.5);
        processDefensiveSUAMod(RAMS, e -> 1.25);
        processDefensiveSUAMod(BHJ2, e -> barFactor * armorThird);
        processDefensiveSUAMod(RCA, e -> barFactor * armorThird);
        processDefensiveSUAMod(SHLD, e -> barFactor * armorThird);
        processDefensiveSUAMod(BHJ3, e -> barFactor * 1.5 * armorThird);
        processDefensiveSUAMod(BRA, e -> barFactor * 0.75 * armorThird);
        processDefensiveSUAMod(IRA, e -> barFactor * 0.5 * armorThird);
        if (element.hasSUA(CR) && (element.getFullStructure() >= 3)) {
            defensiveValue += 0.25;
            report.addLine("Defensive SUA", "+ 0.25 (CR)", "= " + formatForReport(defensiveValue));
        }
        if (element.hasSUA(ARM) && (element.getFullStructure() > 1)) {
            defensiveValue += 0.5;
            report.addLine("Defensive SUA", "+ 0.5 (ARM)", "= " + formatForReport(defensiveValue));
        }
    }

    protected void processDefensiveFactors() {
        report.addLine("- DIR:", "");
        processArmor();
        processStructure();
        double defFactor = getDefenseFactor();
        report.addLine("- DIR:",
                formatForReport(dir) + " x " + formatForReport(defFactor) + ", round to half",
                "= " + formatForReport(0.5 * Math.round(dir * defFactor * 2)));
        dir = 0.5 * Math.round(dir * defFactor * 2);
    }

    protected void processArmor() {
        double armorMultiplier = 2;
        List<String> modifierList = new ArrayList<>();
        if (element.isType(CV, SV)) {
            modifierList.add("MV " + element.getPrimaryMovementMode());
            if (element.getMovementModes().contains("t") || element.getMovementModes().contains("n")
                    || element.getMovementModes().contains("s")) {
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
                element.getFullArmor() + " x " + formatForReport(armorMultiplier) + modifiers,
                "= " + formatForReport(dir));
    }

    protected void processStructure() {
        double strucMultiplier = 1;
        String modifiers = "";
        if (element.isInfantry()) {
            modifiers = " (Infantry)";
            strucMultiplier = 2;
        } else  if (element.isType(IM) || element.hasSUA(BAR)) {
            modifiers = element.isType(IM) ? " (IM)" : " (BAR)";
            strucMultiplier = 0.5;
        }
        dir += element.getFullStructure() * strucMultiplier;
        report.addLine("- Structure",
                "+ " + element.getFullStructure() + " x " + formatForReport(strucMultiplier) + modifiers,
                "= " + formatForReport(dir));
    }

    private double getDefenseFactor() {
        double result = 0;
        double movemod = element.getTMM();
        if (element.isJumpCapable() && (element.isInfantry()
                || (!element.getStandardDamage().hasDamage() && !element.hasAnySUAOf(TSEMP, ARTS,
                ARTAC, ARTAIS, ARTBA, ARTCM12, ARTCM5, ARTCM7, ARTCM9, ARTLT, ARTLTC, ARTSC, ARTT, ARTTC)))) {
            movemod += 1;
        }
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
        if (element.isAerospace()) {
            result += 2;
            modifierList.add("Aerospace");
        }
        if (element.isType(DS, DA)) {
            result -= 2;
            modifierList.add("DropShip");
        }
        if (element.isType(BA, PM)) {
            result += 1;
            modifierList.add(element.getASUnitType().toString());
        }
        if (!element.isMek() && (element.getMovementModes().contains("g")
                || element.getMovementModes().contains("v"))) {
            result++;
            modifierList.add("VTOL/WiGE");
        }
        if (element.hasSUA(STL)) {
            result += 1;
            modifierList.add("STL");
        }
        if (element.hasAnySUAOf(LG, SLG, VLG)) {
            result -= 1;
            modifierList.add("LG/SLG/VLG");
        }
        if (element.hasSUA(JMPS)) {
            result += 0.5 * element.getJMPS();
            modifierList.add("JMPS");
        }
        if (element.hasSUA(SUBS)) {
            result += 0.5 * element.getSUBS();
            modifierList.add("SUBS");
        }

        double multiplier = (result <= 2 ? 0.1 : 0.25);
        double defFactor = 1 + multiplier * result;
        String modifiers = " (" + String.join(", ", modifierList) + ")";
        report.addLine("- Defense Factor",
                "1 + " + formatForReport(multiplier) + " x " + formatForReport(result) + modifiers,
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
            double dmgS = pointValueSDamage(element);
            double dmgM = pointValueMDamage(element);
            double dmgL = pointValueLDamage(element);

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

    /**
     * Determines the Agile Bonus, AlphaStrike Companion Errata v1.4, p.17 and
     * https://bg.battletech.com/forums/errata/alpha-strike-companion/msg1881442/#msg1881442
     */
    private double agileBonus() {
        double result = 0;
        double modifiedTMM = element.getTMM() + 0.5 * element.getJMPS() + 0.5 * element.getSUBS();
        if (modifiedTMM > 1) {
            double dmgS = element.getStandardDamage().S.minimal ? 0.5 : element.getStandardDamage().S.damage;
            double dmgM = element.getStandardDamage().M.minimal ? 0.5 : element.getStandardDamage().M.damage;
            if (dmgM > 0) {
                result = (modifiedTMM - 1) * dmgM;
            } else if (element.getTMM() >= 3) {
                result = (modifiedTMM - 2) * dmgS;
            }
        }
        if (result != 0) {
            report.addLine("Agile", "+ " + result, "");
        }
        return roundToHalf(result);
    }

    /** C3 Bonus, AlphaStrike Companion Errata v1.4, p.17 */
    protected double c3Bonus() {
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
        List<String> modifierList = new ArrayList<>();
        double bonus = 0;
        if (element.hasSUA(AECM)) {
            bonus += 3;
            modifierList.add("AECM");
        }
        if (element.hasSUA(BH)) {
            bonus += 2;
            modifierList.add("BH");
        }
        if (element.hasSUA(C3RS)) {
            bonus += 2;
            modifierList.add("C3RS");
        }
        if (element.hasSUA(ECM)) {
            bonus += 2;
            modifierList.add("ECM");
        }
        if (element.hasSUA(RCN)) {
            bonus += 2;
            modifierList.add("RCN");
        }
        if (element.hasSUA(TRN)) {
            bonus += 2;
            modifierList.add("TRN");
        }
        if (element.hasSUA(LPRB)) {
            bonus += 1;
            modifierList.add("LPRB");
        }
        if (element.hasSUA(PRB)) {
            bonus += 1;
            modifierList.add("PRB");
        }
        if (element.hasSUA(LECM)) {
            bonus += 0.5;
            modifierList.add("LECM");
        }
        if (element.hasSUA(MHQ)) {
            int mhqValue = (int) element.getSUA(MHQ);
            if (mhqValue <= 4) {
                bonus += mhqValue;
            } else {
                bonus += 4 + Math.ceil(0.2 * mhqValue);
            }
            modifierList.add("MHQ");
        }
        if (!modifierList.isEmpty()) {
            String modifiers = " (" + String.join(", ", modifierList) + ")";
            subTotal += bonus;
            report.addLine("Force Bonus", "+ " + bonus + modifiers,
                    "= " + formatForReport(subTotal));
        }
    }

    /** @return The Short Range damage value with minimal damage represented as 0.5 (including TOR). */
    private double pointValueSDamage(AlphaStrikeElement element) {
        return pointValueDamage(element.getStandardDamage().S) + pointValueDamage(element.getTOR().S);
    }

    /** @return The Medium Range damage value with minimal damage represented as 0.5 (including TOR). */
    private double pointValueMDamage(AlphaStrikeElement element) {
        return pointValueDamage(element.getStandardDamage().M) + pointValueDamage(element.getTOR().M);
    }

    /** @return The Long Range damage value with minimal damage represented as 0.5 (including TOR). */
    private double pointValueLDamage(AlphaStrikeElement element) {
        return pointValueDamage(element.getStandardDamage().L) + pointValueDamage(element.getTOR().L);
    }

    /** @return The damage value of the given ASDamage or 0.5 when it's minimal damage. */
    private double pointValueDamage(ASDamage asDamage) {
        return asDamage.minimal ? 0.5 : asDamage.damage;
    }

    /** @return The highest movement capability of any of the element's movement modes. */
    protected int getHighestMove(AlphaStrikeElement element) {
        return element.getMovement().values().stream().mapToInt(m -> m).max().orElse(0);
    }

    double roundToHalf(double number) {
        return 0.5 * Math.round(number * 2);
    }
}