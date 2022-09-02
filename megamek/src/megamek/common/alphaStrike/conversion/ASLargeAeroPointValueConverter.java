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

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
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
        ASDamageVector ftMSL = element.getFrontArc().getMSL();
        ASDamageVector ltMSL = element.getLeftArc().getMSL();
        ASDamageVector rtMSL = element.getRightArc().getMSL();
        ASDamageVector rrMSL = element.getRearArc().getMSL();
        ASDamageVector ftCAP = element.getFrontArc().getCAP();
        ASDamageVector ltCAP = element.getLeftArc().getCAP();
        ASDamageVector rtCAP = element.getRightArc().getCAP();
        ASDamageVector rrCAP = element.getRearArc().getCAP();
        ASDamageVector ftSCP = element.getFrontArc().getSCAP();
        ASDamageVector ltSCP = element.getLeftArc().getSCAP();
        ASDamageVector rtSCP = element.getRightArc().getSCAP();
        ASDamageVector rrSCP = element.getRearArc().getSCAP();
        double stdAndMslDamage = ftStd.S.damage + ftStd.M.damage + ftStd.L.damage;
        report.addLine("STD Damage Front",
                formatForReport(ftStd.S.damage) + " + " + formatForReport(ftStd.M.damage) + " + " + formatForReport(ftStd.L.damage),
                "= " + formatForReport(stdAndMslDamage));
        stdAndMslDamage += ltStd.S.damage + ltStd.M.damage + ltStd.L.damage;
        report.addLine("STD Damage Left",
                "+ " + formatForReport(ltStd.S.damage) + " + " + formatForReport(ltStd.M.damage) + " + " + formatForReport(ltStd.L.damage),
                "= " + formatForReport(stdAndMslDamage));
        stdAndMslDamage += rtStd.S.damage + rtStd.M.damage + rtStd.L.damage;
        report.addLine("STD Damage Right",
                "+ " + formatForReport(rtStd.S.damage) + " + " + formatForReport(rtStd.M.damage) + " + " + formatForReport(rtStd.L.damage),
                "= " + formatForReport(stdAndMslDamage));
        stdAndMslDamage += ftMSL.S.damage + ftMSL.M.damage + ftMSL.L.damage;
        report.addLine("MSL Damage Front",
                "+ " + formatForReport(ftMSL.S.damage) + " + " + formatForReport(ftMSL.M.damage) + " + " + formatForReport(ftMSL.L.damage),
                "= " + formatForReport(stdAndMslDamage));
        stdAndMslDamage += ltMSL.S.damage + ltMSL.M.damage + ltMSL.L.damage;
        report.addLine("MSL Damage Left",
                "+ " + formatForReport(ltMSL.S.damage) + " + " + formatForReport(ltMSL.M.damage) + " + " + formatForReport(ltMSL.L.damage),
                "= " + formatForReport(stdAndMslDamage));
        stdAndMslDamage += rtMSL.S.damage + rtMSL.M.damage + rtMSL.L.damage;
        report.addLine("MSL Damage Right",
                "+ " + formatForReport(rtMSL.S.damage) + " + " + formatForReport(rtMSL.M.damage) + " + " + formatForReport(rtMSL.L.damage),
                "= " + formatForReport(stdAndMslDamage));

        report.addEmptyLine();
        double capAndScapDmg = ftCAP.S.damage + ftCAP.M.damage + ftCAP.L.damage;
        report.addLine("CAP Damage Front",
                formatForReport(ftCAP.S.damage) + " + " + formatForReport(ftCAP.M.damage) + " + " + formatForReport(ftCAP.L.damage),
                "= " + formatForReport(capAndScapDmg));
        capAndScapDmg += ltCAP.S.damage + ltCAP.M.damage + ltCAP.L.damage;
        report.addLine("CAP Damage Left",
                "+ " + formatForReport(ltCAP.S.damage) + " + " + formatForReport(ltCAP.M.damage) + " + " + formatForReport(ltCAP.L.damage),
                "= " + formatForReport(capAndScapDmg));
        capAndScapDmg += rtCAP.S.damage + rtCAP.M.damage + rtCAP.L.damage;
        report.addLine("CAP Damage Right",
                "+ " + formatForReport(rtCAP.S.damage) + " + " + formatForReport(rtCAP.M.damage) + " + " + formatForReport(rtCAP.L.damage),
                "= " + formatForReport(capAndScapDmg));
        capAndScapDmg += ftSCP.S.damage + ftSCP.M.damage + ftSCP.L.damage;
        report.addLine("SCAP Damage Front",
                "+ " + formatForReport(ftSCP.S.damage) + " + " + formatForReport(ftSCP.M.damage) + " + " + formatForReport(ftSCP.L.damage),
                "= " + formatForReport(capAndScapDmg));
        capAndScapDmg += ltSCP.S.damage + ltSCP.M.damage + ltSCP.L.damage;
        report.addLine("SCAP Damage Left",
                "+ " + formatForReport(ltSCP.S.damage) + " + " + formatForReport(ltSCP.M.damage) + " + " + formatForReport(ltSCP.L.damage),
                "= " + formatForReport(capAndScapDmg));
        capAndScapDmg += rtSCP.S.damage + rtSCP.M.damage + rtSCP.L.damage;
        report.addLine("SCAP Damage Right",
                "+ " + formatForReport(rtSCP.S.damage) + " + " + formatForReport(rtSCP.M.damage) + " + " + formatForReport(rtSCP.L.damage),
                "= " + formatForReport(capAndScapDmg));
        if (element.hasMovementMode("a")) {
            offensiveValue = stdAndMslDamage + capAndScapDmg / 4;
            report.addLine("Aerodyne", formatForReport(stdAndMslDamage) + " + " + formatForReport(capAndScapDmg) + " / 4", formatForReport(offensiveValue));
        } else {
            report.addEmptyLine();
            report.addLine("Non-Aerodyne:", "");
            report.addLine("STD Damage Rear",
                    formatForReport(stdAndMslDamage) + " + " + formatForReport(rrStd.S.damage) + " + " + formatForReport(rrStd.M.damage) + " + " + formatForReport(rrStd.L.damage),
                    "= " + formatForReport(stdAndMslDamage + rrStd.S.damage + rrStd.M.damage + rrStd.L.damage));
            stdAndMslDamage += rrStd.S.damage + rrStd.M.damage + rrStd.L.damage;
            stdAndMslDamage += rrMSL.S.damage + rrMSL.M.damage + rrMSL.L.damage;
            report.addLine("MSL Damage Rear",
                    "+ " + formatForReport(rrMSL.S.damage) + " + " + formatForReport(rrMSL.M.damage) + " + " + formatForReport(rrMSL.L.damage),
                    "= " + formatForReport(stdAndMslDamage));

            report.addLine("CAP Damage Rear",
                    formatForReport(capAndScapDmg) + " + " + formatForReport(rrCAP.S.damage) + " + " + formatForReport(rrCAP.M.damage) + " + " + formatForReport(rrCAP.L.damage),
                    "= " + formatForReport(capAndScapDmg + rrCAP.S.damage + rrCAP.M.damage + rrCAP.L.damage));
            capAndScapDmg += rrCAP.S.damage + rrCAP.M.damage + rrCAP.L.damage;
            capAndScapDmg += rrSCP.S.damage + rrSCP.M.damage + rrSCP.L.damage;
            report.addLine("SCAP Damage Rear",
                    "+ " + formatForReport(rrSCP.S.damage) + " + " + formatForReport(rrSCP.M.damage) + " + " + formatForReport(rrSCP.L.damage),
                    "= " + formatForReport(capAndScapDmg));
            offensiveValue = stdAndMslDamage + capAndScapDmg / 5;
            report.addLine("Damage Sum", formatForReport(stdAndMslDamage) + " + " + formatForReport(capAndScapDmg) + " / 5", formatForReport(offensiveValue));

            if (element.isType(WS, DS, SC)) {
                report.addLine("WS/DS/SC", formatForReport(offensiveValue) + " / 4", formatForReport(offensiveValue / 4));
                offensiveValue /= 4;
            }
            if (element.isType(SS, JS, SV)) {
                report.addLine("JS/SS/SV", formatForReport(offensiveValue) + " / 3", formatForReport(offensiveValue / 3));
                offensiveValue /= 3;
            }
        }
    }

    @Override
    protected void processSize() { }

    @Override
    protected void processOffensiveSUAMods() { }

    @Override
    protected void processForceBonus() { }

    @Override
    protected void processOffensiveBlanketMod() {
        report.addLine("Offensive Value:", "", "= " + formatForReport(offensiveValue));
    }

    @Override
    protected void processDefensiveSUAMods() {
        for (ASArcs arc : ASArcs.values()) {
            ASSpecialAbilityCollection arcSummary = element.getArc(arc);
            if (arcSummary.hasSUA(PNT)) {
                int pntValue = (int) arcSummary.getSUA(PNT);
                defensiveValue += pntValue;
                report.addLine("Defensive SPA",
                        "+ " + pntValue + " (PNT, " + arc + ")",
                        "= " + formatForReport(defensiveValue));
            }
        }
        processDefensiveSUAMod(STL, e -> 2.0);
        processDefensiveSUAMod(RCA, e -> {
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
        report.addLine("- Armor", calculation, "= " + formatForReport(dir));
        dir += element.getFullStructure();
        report.addLine("- Structure", "+ " + element.getFullStructure(), "= " + formatForReport(dir));
        dir += 0.5 * element.getThreshold() * element.getSize();
        report.addLine("- Threshold",
                "+ 0.5 x " + element.getThreshold() + " x " + element.getSize(),
                "= " + formatForReport(dir));
    }
}
