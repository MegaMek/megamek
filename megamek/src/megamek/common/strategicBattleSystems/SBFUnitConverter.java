/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.strategicBattleSystems;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.codeUtilities.MathUtility;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.ASUnitType;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.options.OptionsConstants;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.alphaStrike.BattleForceSUA.*;
import static megamek.common.strategicBattleSystems.SBFElementType.*;

public class SBFUnitConverter {

    //TODO: How do MAS/LMAS/STL work? If all have a mix of them, the unit gets all?

    private final Collection<AlphaStrikeElement> elements;
    private final Collection<AlphaStrikeElement> elementsBaseSkill;
    private final SBFUnit unit = new SBFUnit();
    private final CalculationReport report;
    private int roundedAverageMove = 0;

    SBFUnitConverter(Collection<AlphaStrikeElement> elements, String name,
                     Collection<AlphaStrikeElement> elementsBaseSkill, CalculationReport report) {
        this.elements = elements;
        unit.setName(Objects.requireNonNullElse(name, "Unknown"));
        this.elementsBaseSkill = elementsBaseSkill;
        this.report = report;

    }

    /**
     *  Returns an SBF Unit formed from the given AS elements and having the given name.
     *  Does not check validity of the conversion.
     */
    public SBFUnit createSbfUnit() {
        report.addEmptyLine();
        report.addSubHeader("Unit \"" + unit.getName() + "\":");

        if ((elements == null) || (elementsBaseSkill == null) || (elements.isEmpty()) || (elementsBaseSkill.isEmpty())) {
            report.addLine("Error: No elements.", "");
            return unit;
        }

        calcUnitType();
        calcUnitSize();
        calcUnitArmor();
        calcUnitMove();
        setMovementMode();
        calcTransportMove();
        calcUnitJumpMove();
        calcUnitSpecialAbilities();
        calcUnitTMM();

        unit.setDamage(calcUnitDamage(elements, unit));
        calcUnitSkill();
        calcUnitPointValue();
        return unit;
    }

    private void calcUnitType() {
        int majorityCount = (int) Math.round(2.0 / 3 * elements.size());
        List<SBFElementType> types = elements.stream().map(SBFElementType::getUnitType).collect(toList());
        Map<SBFElementType, Long> occurrenceCount = types.stream().collect(groupingBy(Function.identity(), counting()));
        long highestCount = occurrenceCount.values().stream().max(Long::compare).orElse(0L);
        SBFElementType highestType = types.stream()
                .filter(e -> Collections.frequency(types, e) == highestCount)
                .findFirst().orElse(SBFElementType.UNKNOWN);

        if (highestCount < majorityCount) {
            unit.setType(SBFElementType.MX);
        } else {
            unit.setType(highestType);
        }
        report.addLine("Type:",
                "Most frequent: " + highestType + ", " + highestCount + " of " + elements.size(),
                unit.getType().toString());
    }

    private void calcUnitSize() {
        int size = (int) Math.round(elements.stream().mapToInt(AlphaStrikeElement::getSize).average().orElse(0));
        report.addLine("Size:",
                "Average of " + elements.stream().map(u -> u.getSize() + "").collect(joining(", ")),
                size + "");
        unit.setSize(size);
    }

    private void calcUnitTMM() {
        int tmm = getTmmFromMove(roundedAverageMove);
        String calculation = "Average Movement " + roundedAverageMove;
        String moveMode = unit.getMovementMode();
        if (unit.isAnyTypeOf(BA, PM)
                || (unit.isType(V) && (moveMode.equals("v") || moveMode.equals("g")))) {
            tmm += 1;
            calculation += " + 1 (";
            if (unit.isAnyTypeOf(BA, PM)) {
                calculation += unit.getType() + ")";
            } else {
                calculation += moveMode.equals("v") ? "VTOL)" : "WiGE)";
            }
        }
        if (elements.stream().anyMatch(e -> e.hasSUA(LG))) {
            tmm -= 1;
            calculation += " - 1 (LG)";
        }
        if (elements.stream().anyMatch(e -> e.hasAnySUAOf(SLG, VLG))) {
            tmm -= 2;
            calculation += " - 1 (SLG/VLG)";
        }
        if (unit.hasAnySUAOf(STL, MAS)) {
            tmm += 2;
            calculation += " + 2 (STL/MAS)";
        }
        report.addLine("TMM:", calculation, "= " + tmm);
        unit.setTmm(tmm);
    }

    private static ASDamageVector calcUnitDamage(Collection<AlphaStrikeElement> elements, SBFUnit unit) {
        double dmgS = elements.stream().map(AlphaStrikeElement::getStandardDamage).mapToDouble(d -> d.S.asDoubleValue()).sum();
        double dmgM = elements.stream().map(AlphaStrikeElement::getStandardDamage).mapToDouble(d -> d.M.asDoubleValue()).sum();
        double dmgL = elements.stream().map(AlphaStrikeElement::getStandardDamage).mapToDouble(d -> d.L.asDoubleValue()).sum();
        double dmgE = elements.stream().map(AlphaStrikeElement::getStandardDamage).mapToDouble(d -> d.E.asDoubleValue()).sum();
        double artTC = elements.stream().filter(e -> e.hasSUA(ARTTC)).count() * SBFFormation.getSbfArtilleryDamage(ARTTC);
        double artLTC = elements.stream().filter(e -> e.hasSUA(ARTLTC)).count() * SBFFormation.getSbfArtilleryDamage(ARTLTC);
        double artSC = elements.stream().filter(e -> e.hasSUA(ARTSC)).count() * SBFFormation.getSbfArtilleryDamage(ARTSC);
        dmgS += elements.stream().mapToDouble(AlphaStrikeElement::getOV).sum() / 2;
        dmgS += unit.isAnyTypeOf(BA, CI) && unit.hasSUA(AM) ? 1 : 0;
        dmgS += artTC + artLTC + artSC;
        dmgM += elements.stream().filter(e -> e.getStandardDamage().M.damage >= 1).mapToDouble(AlphaStrikeElement::getOV).sum() / 2;
        dmgM += artTC + artLTC + artSC;
        dmgL += elements.stream().filter(e -> e.getStandardDamage().L.damage >= 1).mapToDouble(AlphaStrikeElement::getOV).sum() / 2;
        dmgL += artTC + artLTC;
        dmgE += artTC + artLTC;
        if (unit.getType() == AS) {
            return ASDamageVector.createUpRndDmg(Math.round(dmgS / 3), Math.round(dmgM / 3),
                    Math.round(dmgL / 3), Math.round(dmgE / 3));
        } else {
            return ASDamageVector.createUpRndDmg(Math.round(dmgS / 3), Math.round(dmgM / 3),
                    Math.round(dmgL / 3));
        }
    }

    private void calcUnitSpecialAbilities() {
        report.addLine("Special Abilites:", "");
        addUnitSUAsIfAny(WAT, PRB, AECM, BHJ2, BHJ3, BH, BT, ECM, HPG, LPRB, LECM, TAG);
        addUnitSUAsIfHalf(AMS, ARM, ARS, BAR, BFC, CR, ENG, RBT, SRCH, SHLD);
        addUnitSUAsIfAll(AMP, AM, BHJ, XMEC, MCS, UCS, MEC, PARA, SAW, TRN);
        sumUnitSUAs(CAR, CK, CT, IT, CRW, DCC, MDS, MASH, RSD, VTM, VTH,
                VTS, AT, DT, MT, PT, ST, SCR);
        sumUnitSUAsDivideBy3(ATAC, BOMB, PNT, IF);
        sumUnitArtillery(ARTLT, ARTS, ARTT, ARTBA, ARTCM5, ARTCM7, ARTCM9, ARTCM12);
        calcMHQ();
        calcRCN();
        calcSTL();
        calcOMNI();

        double flkMSum = elements.stream().filter(e -> e.hasSUA(FLK)).map(e -> (ASDamageVector)e.getSUA(FLK)).mapToDouble(dv -> dv.M.asDoubleValue()).sum();
        int flkM = (int) Math.round(flkMSum / 3);
        double flkLSum = elements.stream().filter(e -> e.hasSUA(FLK)).map(e -> (ASDamageVector)e.getSUA(FLK)).mapToDouble(dv -> dv.L.asDoubleValue()).sum();
        int flkL = (int) Math.round(flkLSum / 3);
        if (flkM + flkL > 0) {
            unit.getSpecialAbilities().setSUA(FLK, ASDamageVector.createNormRndDmgNoMin(0, flkM, flkL));
        }

        if (((suaCount(elements, C3M) >= 1) || (suaCount(elements, C3BSM) >= 1))
                && (suaCount(elements, C3M) + suaCount(elements, C3S) + suaCount(elements, C3BSS) >= elements.size() / 2)) {
            unit.getSpecialAbilities().setSUA(AC3);
        }

        if (suaCount(elements, C3I) > 0) {
            String calculation = "C3I: " + suaCount(elements, C3I) + " of " + elements.size() + ", minimum " + (elements.size() / 2);
            String result = "--";
            if (suaCount(elements, C3I) >= elements.size() / 2) {
                unit.getSpecialAbilities().setSUA(AC3);
                result = "C3I";
            }
            report.addLine("", calculation, result);
        }

        if (unit.hasSUA(ATAC)) {
            unit.getSpecialAbilities().replaceSUA(ATAC, Math.round(1.0d / 3 * (int) unit.getSUA(ATAC)));
        }

        finalizeSpecials();
    }

    private void finalizeSpecials() {
        if (unit.hasSUA(PRB)) {
            unit.getSpecialAbilities().removeSUA(LPRB);
        }
        if (unit.hasSUA(AECM)) {
            unit.getSpecialAbilities().removeSUA(LECM);
            unit.getSpecialAbilities().removeSUA(ECM);
        }
        if (unit.hasSUA(ECM)) {
            unit.getSpecialAbilities().removeSUA(LECM);
        }
    }


    /**
     * Returns the number of the given AlphaStrike elements that have the given SUA (regardless of its
     * associated objects)
     */
    private int suaCount(Collection<AlphaStrikeElement> elements, BattleForceSUA SUA) {
        return (int)elements.stream().filter(e -> e.hasSUA(SUA)).count();
    }

    private void addUnitSUAsIfAny(BattleForceSUA... SUAs) {
        addUnitSUAs(1, SUAs);
    }

    private void addUnitSUAsIfHalf(BattleForceSUA... SUAs) {
        addUnitSUAs(Math.max(1, elements.size() / 2), SUAs);
    }

    private void addUnitSUAsIfAll(BattleForceSUA... SUAs) {
        addUnitSUAs(elements.size(), SUAs);
    }

    private void addUnitSUAs(int minimumCount, BattleForceSUA[] SUAs) {
        for (BattleForceSUA sua : SUAs) {
            int count = suaCount(elements, sua);
            if (count >= minimumCount) {
                report.addLine("",
                        sua + ": " + count + " of " + elements.size() + ", minimum " + minimumCount,
                        sua.toString());
                unit.getSpecialAbilities().setSUA(sua);
            } else if (count > 0) {
                report.addLine("",
                        sua + ": " + count + " of " + elements.size() + ", minimum " + minimumCount,
                        "--");
            }
        }
    }

    private void calcOMNI() {
        long omni = elements.stream().filter(e -> e.hasSUA(OMNI)).count();
        if (omni > 0) {
            report.addLine("",
                    "OMNI: " + omni + " of " + elements.size(),
                    "OMNI" + formatForReport(omni));
            unit.getSpecialAbilities().mergeSUA(SBF_OMNI, (int) omni);
        }
    }

    private void sumUnitSUAs(BattleForceSUA... suas) {
        for (BattleForceSUA sua : suas) {
            List<String> summands = new ArrayList<>();
            double sum = 0;
            for (AlphaStrikeElement element : elements) {
                if (element.hasSUA(sua)) {
                    if (element.getSUA(sua) == null) {
                        unit.getSpecialAbilities().mergeSUA(sua, 1);
                        summands.add("1");
                        sum += 1;
                    } else if (element.getSUA(sua) instanceof Integer) {
                        unit.getSpecialAbilities().mergeSUA(sua, (Integer) element.getSUA(sua));
                        summands.add(formatForReport((Integer) element.getSUA(sua)));
                        sum += (Integer) element.getSUA(sua);
                    } else if (element.getSUA(sua) instanceof Double) {
                        unit.getSpecialAbilities().mergeSUA(sua, (Double) element.getSUA(sua));
                        summands.add(formatForReport((Double) element.getSUA(sua)));
                        sum += (Double) element.getSUA(sua);
                    } else if (element.getSUA(sua) instanceof ASDamageVector
                            && ((ASDamageVector)element.getSUA(sua)).rangeBands == 1) {
                        unit.getSpecialAbilities().mergeSUA(sua, ((ASDamageVector) element.getSUA(sua)).S.asDoubleValue());
                        summands.add(formatForReport(((ASDamageVector) element.getSUA(sua)).S.asDoubleValue()));
                        sum += ((ASDamageVector) element.getSUA(sua)).S.asDoubleValue();
                    }
                }
            }
            if (!summands.isEmpty()) {
                report.addLine("",
                        sua + ": " + String.join(" + ", summands),
                        sua + formatForReport(sum));
            }
        }
    }

    private void sumUnitArtillery(BattleForceSUA... suas) {
        for (BattleForceSUA sua : suas) {
            int count = 0;
            List<String> summands = new ArrayList<>();
            for (AlphaStrikeElement element : elements) {
                count += element.hasSUA(sua) ? (int) element.getSUA(sua) : 0;
            }
            summands.add(count + " x " + SBFFormation.getSbfArtilleryDamage(sua));
            double artSum = count * SBFFormation.getSbfArtilleryDamage(sua);
            int value = (int) Math.round(artSum / 3);
            if (artSum > 0) {
                String result = value > 0 ? sua.toString() + value : "--";
                report.addLine("",
                        sua + ": (" + String.join(" + ", summands) + ") / 3, rn",
                        result);
            }
            if (value > 0) {
                unit.getSpecialAbilities().mergeSUA(sua, value);
            }
        }
    }

    private void sumUnitSUAsDivideBy3(BattleForceSUA... suas) {
        for (BattleForceSUA sua : suas) {
            List<String> summands = new ArrayList<>();
            for (AlphaStrikeElement element : elements) {
                if (element.hasSUA(sua)) {
                    if (element.getSUA(sua) == null) {
                        unit.getSpecialAbilities().mergeSUA(sua, 1);
                        summands.add("1");
                    } else if (element.getSUA(sua) instanceof Integer) {
                        unit.getSpecialAbilities().mergeSUA(sua, (Integer) element.getSUA(sua));
                        summands.add(formatForReport((Integer) element.getSUA(sua)));
                    } else if (element.getSUA(sua) instanceof Double) {
                        unit.getSpecialAbilities().mergeSUA(sua, (Double) element.getSUA(sua));
                        summands.add(formatForReport((Double) element.getSUA(sua)));
                    } else if (element.getSUA(sua) instanceof ASDamageVector
                            && ((ASDamageVector)element.getSUA(sua)).rangeBands == 1) {
                        unit.getSpecialAbilities().mergeSUA(sua, ((ASDamageVector) element.getSUA(sua)).S.asDoubleValue());
                        summands.add(formatForReport(((ASDamageVector) element.getSUA(sua)).S.asDoubleValue()));
                    }
                }
            }
            if (unit.hasSUA(sua)) {
                int oneThird = (int) Math.round((double) unit.getSUA(sua) / 3);
                String result = oneThird > 0 ? sua.toString() + oneThird : "--";
                report.addLine("",
                        sua + ": (" + String.join(" + ", summands) + ") / 3, rn",
                        result);
                if (oneThird == 0) {
                    unit.getSpecialAbilities().removeSUA(sua);
                } else {
                    unit.getSpecialAbilities().replaceSUA(sua, oneThird);
                }
            }
        }
    }

    private void calcMHQ() {
        if (elements.stream().noneMatch(e -> e.hasSUA(MHQ))) {
            return;
        }
        int totalMHQ = 0;
        List<String> summands = new ArrayList<>();
        for (AlphaStrikeElement element : elements) {
            if (element.hasSUA(MHQ)) {
                int mhqValue = (int) element.getSUA(MHQ) - 1;
                summands.add(mhqValue + "");
                totalMHQ += mhqValue;
            }
        }
        int oneThird = (int) Math.round((double) totalMHQ / 3);
        String calculation = "MHQ: (" + String.join(" + ", summands) + ") / 3 = "
                + formatForReport((double) totalMHQ / 3) + ", rn";
        String result = oneThird > 0 ? "MHQ" + oneThird : "--";
        if (oneThird > 0) {
            unit.getSpecialAbilities().mergeSUA(MHQ, oneThird);
            report.addLine("", calculation, result);
        }
    }

    private void calcRCN() {
        long rcnCount = elements.stream().filter(e -> e.hasSUA(RCN) || isConsideredRcn(e)).count();
        if (rcnCount >= 2) {
            unit.getSpecialAbilities().setSUA(RCN);
            report.addLine("",
                    "RCN: " + rcnCount + " of " + elements.size() + ", minimum 2",
                    "RCN");
        }
    }

    private void calcSTL() {
        if (elements.stream().filter(e -> e.hasAnySUAOf(STL, MAS, LMAS)).count() == elements.size()) {
            unit.getSpecialAbilities().setSUA(STL);
            unit.getSpecialAbilities().setSUA(MAS);
            unit.getSpecialAbilities().setSUA(LMAS);
        }
    }

    private static boolean isConsideredRcn(AlphaStrikeElement el) {
        return (el.isType(ASUnitType.BM) && el.getSize() <= 2 && el.getPrimaryMovementValue() >= 14)
                || (el.isType(ASUnitType.BM, ASUnitType.PM) && el.getJumpMove() >= 12)
                || (el.isGround() && el.getSize() <= 2 && el.getPrimaryMovementValue() >= 18)
                || el.getName().contains("Scout")
                || el.getName().contains("Recon")
                || el.getName().contains("Sensor")
                || el.hasQuirk(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS);
    }

    private static int getTmmFromMove(int move) {
        if (move >= 18) {
            return 5;
        } else if (move >= 10) {
            return 4;
        } else if (move >= 7) {
            return 3;
        } else if (move >= 5) {
            return 2;
        } else if (move >= 3) {
            return 1;
        } else if (move >= 1) {
            return 0;
        } else {
            return -4;
        }
    }

    /** Determine the Unit's movement value. Must come before TMM. */
    private void calcUnitMove() {
        double averageMove = elements.stream().mapToInt(AlphaStrikeElement::getPrimaryMovementValue).average().orElse(0);
        roundedAverageMove = (int) Math.round(averageMove / 2);
        if (elements.stream().anyMatch(AlphaStrikeElement::isInfantry)) {
            int minInfantryMove = elements.stream()
                    .filter(AlphaStrikeElement::isInfantry)
                    .mapToInt(AlphaStrikeElement::getPrimaryMovementValue)
                    .min()
                    .orElse(0) / 2;
            int finalMove = Math.min(roundedAverageMove, minInfantryMove);
            report.addLine("Movement:", "");
            String elementsSum = elements.stream().map(e -> e.getPrimaryMovementValue() + "").collect(joining(" + "));
            report.addLine("- Average Move", "(" + elementsSum + ") / " + elements.size() + " / 2, rn",
                    "= " + formatForReport(averageMove));
            report.addLine("- Minimum Infantry Move", "", minInfantryMove + "");
            report.addLine("Final Movement Value", "", finalMove + "");
            unit.setMovement(finalMove);
        } else {
            report.addLine("Movement:",
                    "(Average of "
                            + elements.stream().map(e -> e.getPrimaryMovementValue() + "").collect(joining(", "))
                            + ") / 2, rn",
                    formatForReport(roundedAverageMove));
            unit.setMovement(roundedAverageMove);
        }
    }

    private void calcTransportMove() {
        int transportMove = unit.getMovement();
        if (elements.stream().anyMatch(AlphaStrikeElement::isInfantry)) {
            //stub
        }
        unit.setTrspMovement(transportMove);
    }

    private void calcUnitJumpMove() {
        double averageJump = elements.stream().mapToInt(AlphaStrikeElement::getJumpMove).average().orElse(0);
        int jump = (int) Math.round(averageJump / 4);
        report.addLine("Jump:",
                "(Average of "
                        + elements.stream().map(e -> e.getJumpMove() + "").collect(joining(", "))
                        + ") / 4, rn",
                jump + "");
        unit.setJumpMove(jump);
    }

    private void setMovementMode() {
        SBFMoveMode currentMode = new SBFMoveMode("", Integer.MAX_VALUE);
        for (AlphaStrikeElement element : elements) {
            SBFMoveMode newMode = modeForElement(element);
            if (newMode.rank < currentMode.rank) {
                currentMode = newMode;
            }
        }
        report.addLine("Movement Mode:", currentMode.key);
        unit.setMovementMode(currentMode.key);
    }

    private SBFMoveMode modeForElement(AlphaStrikeElement element) {
        if (unit.isAerospace() && element.isGround()) {
            if (element.hasSUA(BIM)) {
                return new SBFMoveMode("l", 50);
            } else if (element.hasSUA(LAM)) {
                return new SBFMoveMode("l", 65);
            } else if (element.hasSUA(SOA)) {
                return new SBFMoveMode("k", 10);
            } else {
                return new SBFMoveMode("unknown", 0);
            }
        }
        switch (element.getPrimaryMovementMode()) {
            case "":
                if (element.isType(ASUnitType.BM, ASUnitType.PM, ASUnitType.IM)) {
                    return new SBFMoveMode("l", 60);
                } else if (element.isType(ASUnitType.WS)) {
                    return new SBFMoveMode("aw", 31);
                } else if (element.isType(ASUnitType.BA)) {
                    return new SBFMoveMode("l", 50);
                }
                break;
            case "w":
            case "w(b)":
            case "w(m)":
            case "m":
                return new SBFMoveMode("w", 20);
            case "v":
                return new SBFMoveMode("v", 80);
            case "f":
                return new SBFMoveMode("f", 52);
            case "i":
                return new SBFMoveMode("i", 41);
            case "r":
                return new SBFMoveMode("r", 10);
            case "h":
                return new SBFMoveMode("h", 30);
            case "g":
                return new SBFMoveMode("g", 81);
            case "p":
                return new SBFMoveMode("p", 21);
            case "a":
                return new SBFMoveMode("a", 54);
            case "qt":
                return new SBFMoveMode("qt", 63);
            case "qw":
                return new SBFMoveMode("qw", 64);
            case "k":
                return new SBFMoveMode("k", 11);
            case "n":
                return new SBFMoveMode("n", 0);
            case "t":
                return new SBFMoveMode("t", 40);
            case "s":
                if (element.isType(ASUnitType.CV, ASUnitType.SV)) {
                    return new SBFMoveMode("s", 0);
                } else if (element.isType(ASUnitType.BM, ASUnitType.PM)) {
                    return new SBFMoveMode("s", 62);
                } else if (element.isType(ASUnitType.BA)) {
                    return new SBFMoveMode("s", 51);
                }
                break;
            case "j":
                if (element.isType(ASUnitType.BM, ASUnitType.PM)) {
                    return new SBFMoveMode("j", 70);
                } else if (element.isType(ASUnitType.BA)) {
                    return new SBFMoveMode("j", 61);
                } else if (element.isType(ASUnitType.CI)) {
                    return new SBFMoveMode("j", 53);
                }
                break;
        }
        return new SBFMoveMode("unknown", 0);
    }

    private static class SBFMoveMode {
        String key;
        int rank;

        SBFMoveMode(String key, int rank) {
            this.key = key;
            this.rank = rank;
        }
    }

    private void calcUnitArmor() {
        report.addLine("Armor:", "");
        double result = 0;
        for (AlphaStrikeElement element : elements) {
            String modifier = "";
            double delta = 0;
            double elementArmorValue = element.getFullArmor() + element.getFullStructure();
            String calculation = element.getFullArmor() + " + " + element.getFullStructure();
            if (element.getFullStructure() >= 3 || element.hasAnySUAOf(AMS, CASE)) {
                delta = 0.5;
                if (element.getFullStructure() >= 3) {
                    modifier = "Str3+";
                } else {
                    modifier = element.hasSUA(AMS) ? "AMS" : "CASE";
                }
            }
            if (element.hasAnySUAOf(ENE, CASEII, CR, RAMS)) {
                delta = 1;
                if (element.hasSUA(ENE)) {
                    modifier = "ENE";
                } else if (element.hasSUA(CASEII)) {
                    modifier = "CASEII";
                } else {
                    modifier = element.hasSUA(CR) ? "CR" : "RAMS";
                }
            }
            if (delta > 0) {
                calculation += " + " + formatForReport(delta) + " (" + modifier + ")";
                elementArmorValue += delta;
            }
            report.addLine("- " + element.getName(), calculation, "= " + formatForReport(elementArmorValue));
            result += elementArmorValue;
        }
        int armor = (int) Math.round(result / 3);
        report.addLine("- Armor:",
                formatForReport(result) + " / 3, rn",
                "= " + formatForReport(armor));
        unit.setArmor(armor);
    }

    private void calcUnitPointValue() {
        double sum = (double) elementsBaseSkill.stream().mapToInt(AlphaStrikeElement::getPointValue).sum() / 3;
        int intermediate = (int) Math.round(sum);
        String calculation = "(" + elementsBaseSkill.stream().map(e -> e.getPointValue() + "")
                .collect(joining(" + ")) + ") / 3 = " + formatForReport(sum) + ", rn";
        double result = intermediate;
        String skillCalculation = "";
        if (unit.getSkill() > 4) {
            result = (1.0d - (unit.getSkill() - 4) * 0.1) * intermediate;
            skillCalculation += intermediate + " x (1 - (" + unit.getSkill() + " - 4) x 0.1) = " + result + ", rn";
        } else if (unit.getSkill() < 4) {
            result = (1.0d + (4 - unit.getSkill()) * 0.2) * intermediate;
            skillCalculation += intermediate + " x (1 + (4 - " + unit.getSkill() + ") x 0.2) = " + result + ", rn, Min "
                    + (intermediate + 4 - unit.getSkill());
            result = Math.max(intermediate + 4 - unit.getSkill(), result);
        }
        int pointValue = Math.max(1, (int) Math.round(result));
        if (unit.getSkill() == 4) {
            report.addLine("Point Value:", calculation, "= " + intermediate);
        } else {
            report.addLine("Point Value:", "");
            report.addLine("- PV Sum", calculation, "= " + intermediate);
            report.addLine("- Skill Modifier", skillCalculation, "= " + pointValue);
        }
        unit.setPointValue(pointValue);
    }

    private void calcUnitSkill() {
        int skill = (int) Math.round(elements.stream().mapToInt(AlphaStrikeElement::getSkill).average().orElse(4));
        String calculation = "(" + elements.stream().map(e -> e.getSkill() + "").collect(joining(" + ")) + ") / " + elements.size();
        List<String> modifiers = new ArrayList<>();
        if (unit.hasSUA(DN)) {
            skill -= 1;
            calculation += " - 1";
            modifiers.add("DN");
        }
        if (unit.hasAnySUAOf(BFC, DRO, RBT)) {
            skill += 1;
            calculation += " + 1";
            if (unit.hasSUA(BFC)) {
                modifiers.add("BFC");
            } else {
                modifiers.add(unit.hasSUA(DRO) ? "DRO" : "RBT");
            }
        }
        if (!modifiers.isEmpty()) {
            calculation += "(" + String.join(", ", modifiers) + ")";
        }
        calculation += ", rn, [0, 7]";
        skill = MathUtility.clamp(skill, 0, 7);
        report.addLine("Skill:", calculation, skill + "");
        unit.setSkill(skill);
    }
}