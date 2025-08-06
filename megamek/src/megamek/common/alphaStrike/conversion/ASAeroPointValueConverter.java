/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.alphaStrike.conversion;

import static megamek.client.ui.clientGUI.calculationReport.CalculationReport.formatForReport;
import static megamek.common.alphaStrike.BattleForceSUA.BAR;
import static megamek.common.alphaStrike.BattleForceSUA.BT;
import static megamek.common.alphaStrike.BattleForceSUA.PNT;
import static megamek.common.alphaStrike.BattleForceSUA.RCA;
import static megamek.common.alphaStrike.BattleForceSUA.STL;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.alphaStrike.AlphaStrikeElement;

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
            double armorThird = Math.floor((double) element.getFullArmor() / 3);
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
