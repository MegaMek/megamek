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

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.FixedWingSupport;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.Warship;

final class ASSizeConverter {

    /**
     * Determines the element's size, AlphaStrike Companion, p.92
     */
    static int convertSize(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity;
        CalculationReport report = conversionData.conversionReport;

        if ((entity instanceof Tank) && entity.isSupportVehicle()) {
            if (entity.getWeight() < 5) {
                report.addLine("Size:", "Ground SV, weight < 5t", "1");
                return 1;
            }
            int mediumCeil = 0;
            int largeCeil = 0;
            int veryLargeCeil = 0;
            switch (entity.getMovementMode()) {
                case TRACKED:
                    mediumCeil = 100;
                    largeCeil = 200;
                    break;
                case WHEELED:
                    mediumCeil = 80;
                    largeCeil = 160;
                    break;
                case HOVER:
                    mediumCeil = 50;
                    largeCeil = 100;
                    break;
                case NAVAL:
                case HYDROFOIL:
                case SUBMARINE:
                    mediumCeil = 300;
                    largeCeil = 6000;
                    veryLargeCeil = 30000;
                    break;
                case WIGE:
                    mediumCeil = 80;
                    largeCeil = 240;
                    break;
                case RAIL:
                    mediumCeil = 300;
                    largeCeil = 600;
                    break;
                case AIRSHIP:
                    mediumCeil = 300;
                    largeCeil = 600;
                    veryLargeCeil = 900;
                    break;
                case VTOL:
                    mediumCeil = 30;
                    largeCeil = 60;
                default:
                    break;
            }
            if (entity.getWeight() <= mediumCeil) {
                report.addLine("Size:", "Ground SV, weight <= " + mediumCeil + "t", "2");
                return 2;
            } else if (entity.getWeight() <= largeCeil) {
                report.addLine("Size:", "Ground SV, " + mediumCeil + "t < weight <= " + largeCeil + "t", "3");
                return 3;
            } else if ((entity.getWeight() <= veryLargeCeil) || (veryLargeCeil == 0)) {
                String calculation = "Ground SV, " + ((veryLargeCeil == 0) ? "weight > " + largeCeil + "t" :
                      largeCeil + "t < weight <= " + veryLargeCeil + "t");
                report.addLine("Size:", calculation, "4");
                return 4;
            } else {
                report.addLine("Size:", "Ground SV, weight > " + veryLargeCeil + "t", "5");
                return 5;
            }

        } else if (entity instanceof Infantry) {
            report.addLine("Size:", "Infantry", "1");
            return 1;

        } else if (entity instanceof Warship) {
            if (entity.getWeight() < 500000) {
                report.addLine("Size:", "WarShip < 500000t", "1");
                return 1;
            } else if (entity.getWeight() < 800000) {
                report.addLine("Size:", "WarShip < 800000t", "2");
                return 2;
            } else if (entity.getWeight() < 1200000) {
                report.addLine("Size:", "WarShip < 1200000t", "3");
                return 3;
            } else {
                report.addLine("Size:", "WarShip >= 1200000t", "4");
                return 4;
            }

        } else if (entity instanceof Jumpship) {
            if (entity.getWeight() < 100000) {
                report.addLine("Size:", "Jumpship < 100000t", "1");
                return 1;
            } else if (entity.getWeight() < 300000) {
                report.addLine("Size:", "Jumpship < 300000t", "2");
                return 2;
            } else {
                report.addLine("Size:", "Jumpship >= 300000t", "3");
                return 3;
            }

        } else if (entity instanceof SmallCraft) {
            if (entity.getWeight() < 2500) {
                report.addLine("Size:", "SmallCraft < 2500t", "1");
                return 1;
            } else if (entity.getWeight() < 10000) {
                report.addLine("Size:", "SmallCraft < 10000t", "2");
                return 2;
            } else {
                report.addLine("Size:", "SmallCraft >= 10000t", "3");
                return 3;
            }

        } else if (entity instanceof FixedWingSupport) {
            if (entity.getWeight() < 5) {
                report.addLine("Size:", "Fixed Wing SV, weight < 5t", "1");
                return 1;
            } else if (entity.getWeight() <= 100) {
                report.addLine("Size:", "Fixed Wing SV, 5t < weight <= 100t", "2");
                return 2;
            } else {
                report.addLine("Size:", "Fixed Wing SV, weight > 100t", "3");
                return 3;
            }

        } else if (entity instanceof Aero) {
            if (entity.getWeight() < 50) {
                report.addLine("Size:", "Aero Fighter, weight < 50t", "1");
                return 1;
            } else if (entity.getWeight() < 75) {
                report.addLine("Size:", "Aero Fighter, 50t <= weight < 75t", "2");
                return 2;
            } else {
                report.addLine("Size:", "Aero Fighter, weight >= 75t", "3");
                return 3;
            }

        } else {
            if (entity.getWeight() < 40) {
                report.addLine("Size:", "Weight < 40t", "1");
                return 1;
            } else if (entity.getWeight() < 60) {
                report.addLine("Size:", "40t <= weight < 60t", "2");
                return 2;
            } else if (entity.getWeight() < 80) {
                report.addLine("Size:", "60t <= weight < 80t", "3");
                return 3;
            } else {
                report.addLine("Size:", "Weight >= 80t", "4");
                return 4;
            }
        }
    }

    // Make non-instantiable
    private ASSizeConverter() {}
}
