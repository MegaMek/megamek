/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import megamek.client.bot.princess.geometry.ConvexBoardArea;
import megamek.client.bot.princess.geometry.HexLine;
import megamek.common.Coords;
import megamek.logging.MMLogger;

/**
 * This contains useful classes and functions for geometric questions the bot algorithm might have
 */
public class BotGeometry {
    private final static MMLogger LOGGER = MMLogger.create(BotGeometry.class);

    /**
     * runs a series of self tests to make sure geometry is done correctly
     */
    static void debugSelfTest(Princess owner) {
        final String PASSED = "passed";
        final String FAILED = "failed";

        StringBuilder msg = new StringBuilder("Performing self test of geometry");

        try {
            Coords center = new Coords(4, 6);
            HexLine[] lines = new HexLine[6];
            for (int i = 0; i < 6; i++) {
                lines[i] = new HexLine(center, i);
            }

            msg.append("\n\tTesting that center lies in lines... ");
            boolean passed = true;
            for (int i = 0; i < 6; i++) {
                if (lines[i].judgePoint(center) != 0) {
                    passed = false;
                }
            }
            msg.append(passed ? PASSED : FAILED);

            msg.append("\n\tTesting more points that should lie on lines... ");
            passed = true;
            for (int i = 0; i < 6; i++) {
                if ((lines[i].judgePoint(center.translated(i)) != 0) || (lines[i].judgePoint(center.translated((i +
                      3) %
                      6)) != 0)) {
                    passed = false;
                }
            }
            msg.append(passed ? PASSED : FAILED);

            passed = true;
            msg.append("\n\tTesting points to left and right of lines... ");
            for (int i = 0; i < 6; i++) {
                if (-1 != lines[i].judgePoint(center.translated((i + 5) % 6))) {
                    passed = false;
                }
                if (-1 != lines[i].judgePoint(center.translated((i + 4) % 6))) {
                    passed = false;
                }
                if (1 != lines[i].judgePoint(center.translated((i + 1) % 6))) {
                    passed = false;
                }
                if (1 != lines[i].judgePoint(center.translated((i + 2) % 6))) {
                    passed = false;
                }
            }
            msg.append(passed ? PASSED : FAILED);

            passed = true;
            Coords areaPoint1 = new Coords(1, 1);
            Coords areaPoint2 = new Coords(3, 1);
            Coords areaPoint3 = new Coords(2, 3);
            ConvexBoardArea area = new ConvexBoardArea();
            area.expandToInclude(areaPoint1);
            area.expandToInclude(areaPoint2);
            area.expandToInclude(areaPoint3);
            LOGGER.debug("Checking area contains proper points... ");
            msg.append("\n\tChecking area contains proper points... ");
            if (!area.contains(new Coords(1, 1))) {
                passed = false;
            }
            if (!area.contains(new Coords(2, 1))) {
                passed = false;
            }
            if (!area.contains(new Coords(3, 1))) {
                passed = false;
            }
            if (!area.contains(new Coords(1, 2))) {
                passed = false;
            }
            if (!area.contains(new Coords(2, 2))) {
                passed = false;
            }
            if (!area.contains(new Coords(3, 2))) {
                passed = false;
            }
            if (!area.contains(new Coords(2, 3))) {
                passed = false;
            }
            msg.append(passed ? PASSED : FAILED);

            passed = true;
            msg.append("\n\tChecking area doesn't contain extra points... ");
            if (area.contains(new Coords(0, 1))) {
                passed = false;
            }
            if (area.contains(new Coords(1, 0))) {
                passed = false;
            }
            if (area.contains(new Coords(2, 0))) {
                passed = false;
            }
            if (area.contains(new Coords(3, 0))) {
                passed = false;
            }
            if (area.contains(new Coords(4, 1))) {
                passed = false;
            }
            if (area.contains(new Coords(4, 2))) {
                passed = false;
            }
            if (area.contains(new Coords(4, 3))) {
                passed = false;
            }
            if (area.contains(new Coords(3, 3))) {
                passed = false;
            }
            if (area.contains(new Coords(2, 4))) {
                passed = false;
            }
            if (area.contains(new Coords(1, 3))) {
                passed = false;
            }
            if (area.contains(new Coords(0, 3))) {
                passed = false;
            }
            if (area.contains(new Coords(0, 2))) {
                passed = false;
            }
            msg.append(passed ? PASSED : FAILED);

        } finally {
            LOGGER.debug(msg.toString());
        }
    }
}
