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
import megamek.common.alphaStrike.AlphaStrikeElement;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASAeroPointValueConverter extends ASPointValueConverter {

    ASAeroPointValueConverter(AlphaStrikeElement element, CalculationReport report) {
        super(element, report);
    }

    @Override
    protected void processMovement() {
        int highestMove = getHighestMove(element);
        defensiveValue += 4 + 0.25 * highestMove;
        report.addLine("Movement", "4 + " + highestMove + " / 4", "" + formatForReport(defensiveValue));
        if (highestMove >= 10) {
            defensiveValue += 1;
            report.addLine("Thrust", "+1 (Very High Thrust)", "= " + formatForReport(defensiveValue));
        } else if (highestMove >= 7) {
            defensiveValue += 0.5;
            report.addLine("Thrust", "+0.5 (High Thrust)", "= " + formatForReport(defensiveValue));
        }
    }

    @Override
    protected void processOffensiveBT() {
        processOffensiveSUAMod(BT, e -> (double) element.getSize());
    }

    @Override
    protected void processDefensiveSUAMods() {
        processDefensiveSUAMod(PNT, e -> (double) (int) element.getSUA(PNT));
        processDefensiveSUAMod(STL, e -> 2.0);
        processDefensiveSUAMod(RCA, e -> {
            double armorThird = Math.floor((double)element.getFullArmor() / 3);
            double barFactor = element.hasSUA(BAR) ? 0.5 : 1;
            return armorThird * barFactor;
        });
    }

    @Override
    protected void processArmor() {
        String calculation = element.getFullArmor() + "";
        double barFactor = 1;
        if (element.hasSUA(BAR)) {
            barFactor = 0.5;
            calculation += " / 2 (BAR)";
        }
        double thresholdMultiplier = Math.min(1.3 + 0.1 * element.getThreshold(), 1.9);
        calculation += " x " + formatForReport(thresholdMultiplier);
        dir += thresholdMultiplier * barFactor * element.getFullArmor();
        report.addLine("- Armor", calculation, "= " + formatForReport(dir));
    }

    @Override
    protected void processStructure() {
        dir += element.getFullStructure();
        report.addLine("- Structure", "+ " + element.getFullStructure(), "= " + formatForReport(dir));
    }

    @Override
    protected void processGroundCapabilities() {
        subTotal += c3Bonus();
    }
}
