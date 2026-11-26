/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.server.totalWarfare;

import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.equipment.ObjectiveMarker;
import megamek.logging.MMLogger;
import megamek.server.victory.VictoryPointVictory;

/**
 * Exposes False objectives (decoys). A decoy looks like any other objective until a scan reads it or until the moment
 * it would pay; then everyone is told it was a decoy, nobody scores it, and it leaves the board. Both the scan pass
 * and the control resolution expose decoys through here, so the two reveals read and behave the same.
 */
class ObjectiveDecoyHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(ObjectiveDecoyHandler.class);

    /** A scan read the decoy. */
    static final int REPORT_DECOY_SCANNED = 7159;
    /** The decoy reached the moment it would have paid. */
    static final int REPORT_DECOY_AT_PAYOUT = VictoryPointVictory.REPORT_DECOY_AT_PAYOUT;

    ObjectiveDecoyHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Tells everyone the objective was a decoy, removes it from the board and sends the clients the new board.
     *
     * @param decoy    the False objective
     * @param position where it is
     * @param reportId {@link #REPORT_DECOY_SCANNED} or {@link #REPORT_DECOY_AT_PAYOUT}
     */
    void exposeDecoy(ObjectiveMarker decoy, Coords position, int reportId) {
        getGame().removeGroundObject(position, decoy);
        Report report = new Report(reportId, Report.PUBLIC);
        report.add(decoy.generalName());
        report.add(position.getBoardNum());
        addReport(report);
        gameManager.sendGroundObjectUpdate();
        LOGGER.info("[Objective] {} at {} exposed as a decoy ({}) and removed", decoy.generalName(),
              position.getBoardNum(), (reportId == REPORT_DECOY_SCANNED) ? "scanned" : "it would have paid");
    }
}
