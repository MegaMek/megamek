/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.server.totalwarfare;

import megamek.common.Game;
import megamek.common.Report;

import java.util.Vector;

/**
 * Classes working closely with TWGameManager can extend this class for less verbose access to report methods and
 * the Game instance.
 */
abstract class AbstractTWRuleHandler  {

    final TWGameManager gameManager;

    AbstractTWRuleHandler(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    void addReport(Report report) {
        gameManager.addReport(report);
    }

    void addReport(Vector<Report> reports) {
        gameManager.addReport(reports);
    }

    void addReport(Vector<Report> reports, int indent) {
        gameManager.addReport(reports, indent);
    }

    void addNewLines() {
        gameManager.addNewLines();
    }

    Game getGame() {
        return gameManager.getGame();
    }
}
