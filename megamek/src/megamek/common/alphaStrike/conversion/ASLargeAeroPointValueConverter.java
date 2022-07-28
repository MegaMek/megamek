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
import megamek.common.alphaStrike.ASArcSummary;
import megamek.common.alphaStrike.ASArcs;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.AlphaStrikeElement;

import static megamek.client.ui.swing.calculationReport.CalculationReport.fmt;
import static megamek.common.alphaStrike.ASUnitType.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASLargeAeroPointValueConverter extends ASAeroPointValueConverter {

    ASLargeAeroPointValueConverter(AlphaStrikeElement element, CalculationReport report) {
        super(element, report);
    }

    @Override
    protected void processStandardDamageAndOV() {
        ASDamageVector ftStd = element.getFrontArc().getStdDamage();
        ASDamageVector ltStd = element.getLeftArc().getStdDamage();
        ASDamageVector rtStd = element.getRightArc().getStdDamage();
        ASDamageVector rrStd = element.getRearArc().getStdDamage();
        ASDamageVector ftMSL = element.getFrontArc().getMSLDamage();
        ASDamageVector ltMSL = element.getLeftArc().getMSLDamage();
        ASDamageVector rtMSL = element.getRightArc().getMSLDamage();
        ASDamageVector rrMSL = element.getRearArc().getMSLDamage();
        ASDamageVector ftCAP = element.getFrontArc().getCAPDamage();
        ASDamageVector ltCAP = element.getLeftArc().getCAPDamage();
        ASDamageVector rtCAP = element.getRightArc().getCAPDamage();
        ASDamageVector rrCAP = element.getRearArc().getCAPDamage();
        ASDamageVector ftSCP = element.getFrontArc().getSCAPDamage();
        ASDamageVector ltSCP = element.getLeftArc().getSCAPDamage();
        ASDamageVector rtSCP = element.getRightArc().getSCAPDamage();
        ASDamageVector rrSCP = element.getRearArc().getSCAPDamage();
        double stdAndMslDamage = ftStd.S.damage + ftStd.M.damage + ftStd.L.damage;
        report.addLine("STD Damage Front",
                fmt(ftStd.S.damage) + " + " + fmt(ftStd.M.damage) + " + " + fmt(ftStd.L.damage),
                "= " + fmt(stdAndMslDamage));
        stdAndMslDamage += ltStd.S.damage + ltStd.M.damage + ltStd.L.damage;
        report.addLine("STD Damage Left",
                "+ " + fmt(ltStd.S.damage) + " + " + fmt(ltStd.M.damage) + " + " + fmt(ltStd.L.damage),
                "= " + fmt(stdAndMslDamage));
        stdAndMslDamage += rtStd.S.damage + rtStd.M.damage + rtStd.L.damage;
        report.addLine("STD Damage Right",
                "+ " + fmt(rtStd.S.damage) + " + " + fmt(rtStd.M.damage) + " + " + fmt(rtStd.L.damage),
                "= " + fmt(stdAndMslDamage));
        stdAndMslDamage += ftMSL.S.damage + ftMSL.M.damage + ftMSL.L.damage;
        report.addLine("MSL Damage Front",
                "+ " + fmt(ftMSL.S.damage) + " + " + fmt(ftMSL.M.damage) + " + " + fmt(ftMSL.L.damage),
                "= " + fmt(stdAndMslDamage));
        stdAndMslDamage += ltMSL.S.damage + ltMSL.M.damage + ltMSL.L.damage;
        report.addLine("MSL Damage Left",
                "+ " + fmt(ltMSL.S.damage) + " + " + fmt(ltMSL.M.damage) + " + " + fmt(ltMSL.L.damage),
                "= " + fmt(stdAndMslDamage));
        stdAndMslDamage += rtMSL.S.damage + rtMSL.M.damage + rtMSL.L.damage;
        report.addLine("MSL Damage Right",
                "+ " + fmt(rtMSL.S.damage) + " + " + fmt(rtMSL.M.damage) + " + " + fmt(rtMSL.L.damage),
                "= " + fmt(stdAndMslDamage));

        report.addEmptyLine();
        double capAndScapDmg = ftCAP.S.damage + ftCAP.M.damage + ftCAP.L.damage;
        report.addLine("CAP Damage Front",
                fmt(ftCAP.S.damage) + " + " + fmt(ftCAP.M.damage) + " + " + fmt(ftCAP.L.damage),
                "= " + fmt(capAndScapDmg));
        capAndScapDmg += ltCAP.S.damage + ltCAP.M.damage + ltCAP.L.damage;
        report.addLine("CAP Damage Left",
                "+ " + fmt(ltCAP.S.damage) + " + " + fmt(ltCAP.M.damage) + " + " + fmt(ltCAP.L.damage),
                "= " + fmt(capAndScapDmg));
        capAndScapDmg += rtCAP.S.damage + rtCAP.M.damage + rtCAP.L.damage;
        report.addLine("CAP Damage Right",
                "+ " + fmt(rtCAP.S.damage) + " + " + fmt(rtCAP.M.damage) + " + " + fmt(rtCAP.L.damage),
                "= " + fmt(capAndScapDmg));
        capAndScapDmg += ftSCP.S.damage + ftSCP.M.damage + ftSCP.L.damage;
        report.addLine("SCAP Damage Front",
                "+ " + fmt(ftSCP.S.damage) + " + " + fmt(ftSCP.M.damage) + " + " + fmt(ftSCP.L.damage),
                "= " + fmt(capAndScapDmg));
        capAndScapDmg += ltSCP.S.damage + ltSCP.M.damage + ltSCP.L.damage;
        report.addLine("SCAP Damage Left",
                "+ " + fmt(ltSCP.S.damage) + " + " + fmt(ltSCP.M.damage) + " + " + fmt(ltSCP.L.damage),
                "= " + fmt(capAndScapDmg));
        capAndScapDmg += rtSCP.S.damage + rtSCP.M.damage + rtSCP.L.damage;
        report.addLine("SCAP Damage Right",
                "+ " + fmt(rtSCP.S.damage) + " + " + fmt(rtSCP.M.damage) + " + " + fmt(rtSCP.L.damage),
                "= " + fmt(capAndScapDmg));
        if (element.hasMovementMode("a")) {
            offensiveValue = stdAndMslDamage + capAndScapDmg / 4;
            report.addLine("Aerodyne", fmt(stdAndMslDamage) + " + " + fmt(capAndScapDmg) + " / 4", fmt(offensiveValue));
        } else {
            report.addEmptyLine();
            report.addLine("Non-Aerodyne:", "");
            report.addLine("STD Damage Rear",
                    fmt(stdAndMslDamage) + " + " + fmt(rrStd.S.damage) + " + " + fmt(rrStd.M.damage) + " + " + fmt(rrStd.L.damage),
                    "= " + fmt(stdAndMslDamage + rrStd.S.damage + rrStd.M.damage + rrStd.L.damage));
            stdAndMslDamage += rrStd.S.damage + rrStd.M.damage + rrStd.L.damage;
            stdAndMslDamage += rrMSL.S.damage + rrMSL.M.damage + rrMSL.L.damage;
            report.addLine("MSL Damage Rear",
                    "+ " + fmt(rrMSL.S.damage) + " + " + fmt(rrMSL.M.damage) + " + " + fmt(rrMSL.L.damage),
                    "= " + fmt(stdAndMslDamage));

            report.addLine("CAP Damage Rear",
                    fmt(capAndScapDmg) + " + " + fmt(rrCAP.S.damage) + " + " + fmt(rrCAP.M.damage) + " + " + fmt(rrCAP.L.damage),
                    "= " + fmt(capAndScapDmg + rrCAP.S.damage + rrCAP.M.damage + rrCAP.L.damage));
            capAndScapDmg += rrCAP.S.damage + rrCAP.M.damage + rrCAP.L.damage;
            capAndScapDmg += rrSCP.S.damage + rrSCP.M.damage + rrSCP.L.damage;
            report.addLine("SCAP Damage Rear",
                    "+ " + fmt(rrSCP.S.damage) + " + " + fmt(rrSCP.M.damage) + " + " + fmt(rrSCP.L.damage),
                    "= " + fmt(capAndScapDmg));
            offensiveValue = stdAndMslDamage + capAndScapDmg / 5;
            report.addLine("Damage Sum", fmt(stdAndMslDamage) + " + " + fmt(capAndScapDmg) + " / 5", fmt(offensiveValue));

            if (element.isType(WS, DS, SC)) {
                report.addLine("WS/DS/SC", fmt(offensiveValue) + " / 4", fmt(offensiveValue / 4));
                offensiveValue /= 4;
            }
            if (element.isType(SS, JS, SV)) {
                report.addLine("JS/SS/SV", fmt(offensiveValue) + " / 3", fmt(offensiveValue / 3));
                offensiveValue /= 3;
            }
        }
    }

    @Override
    protected void processMovement() {
        int highestMove = getHighestMove(element);
        defensiveValue += 0.25 * highestMove;
        report.addLine("Movement", highestMove + " / 4", "" + fmt(defensiveValue));
        if (highestMove >= 10) {
            defensiveValue += 2;
            report.addLine("Thrust", "+2 (Very High Thrust)", "= " + fmt(defensiveValue));
        } else if (highestMove >= 7) {
            defensiveValue += 0.5;
            report.addLine("Thrust", "+0.5 (High Thrust)", "= " + fmt(defensiveValue));
        }
    }

    @Override
    protected void processSize() { }

    @Override
    protected void processOffensiveSPAMods() { }

    @Override
    protected void processForceBonus() { }

    @Override
    protected void processOffensiveBlanketMod() {
        report.addLine("Offensive Value:", "", "= " + fmt(offensiveValue));
    }

    @Override
    protected void processDefensiveSPAMods() {
        for (ASArcs arc : ASArcs.values()) {
            ASArcSummary arcSummary = element.getArc(arc);
            if (arcSummary.hasSPA(PNT)) {
                int pntValue = (int) arcSummary.getSPA(PNT);
                defensiveValue += pntValue;
                report.addLine("Defensive SPA",
                        "+ " + pntValue + " (PNT, " + arc + ")",
                        "= " + fmt(defensiveValue));
            }
        }
        processDefensiveSPAMod(STL, e -> 2.0);
        processDefensiveSPAMod(RCA, e -> {
            double armorThird = Math.floor((double)element.getFullArmor() / 3);
            double barFactor = element.hasSUA(BAR) ? 0.5 : 1;
            return armorThird * barFactor;
        });
    }

    @Override
    protected void processDefensiveFactors() {
        String calculation = element.getFullArmor() + " x 1.5";
        double barFactor = 1;
        if (element.hasSUA(BAR)) {
            barFactor = 0.5;
            calculation += " / 2 (BAR)";
        }
        dir += 1.5 * barFactor * element.getFullArmor();
        report.addLine("- Armor", calculation, "= " + fmt(dir));
        dir += element.getFullStructure();
        report.addLine("- Structure", "+ " + element.getFullStructure(), "= " + fmt(dir));
        dir += 0.5 * element.getThreshold() * element.getSize();
        report.addLine("- Threshold",
                "+ 0.5 x " + element.getThreshold() + " x " + element.getSize(),
                "= " + fmt(dir));
    }
}
