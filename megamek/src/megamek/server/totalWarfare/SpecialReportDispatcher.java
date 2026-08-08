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

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import megamek.common.Report;
import megamek.logging.MMLogger;

/**
 * Sends mid-phase "special report" packets containing only the reports a given player has not received yet.
 *
 * <p>Several rules paths interrupt a movement phase to show the player what just happened rather than making them
 * wait for the end-of-phase report: a hidden unit being revealed, a point-blank shot, an interrupted turn. Each of
 * those used to send the <em>entire</em> phase report accumulated so far, so the second interruption in a phase
 * re-sent everything the first one had already delivered, the third re-sent both, and so on. Clients that append
 * what they receive - the toast overlay and the accessibility log - showed those events again each time.</p>
 *
 * <p>This class keeps a per-player high-water mark into {@link TWGameManager#getMainPhaseReport()} and sends only
 * the tail past it. The mark has to be per player because a single interruption can report to two players at once
 * (the unit that was revealed and the unit that spotted it), and those two are generally not up to date with each
 * other.</p>
 */
class SpecialReportDispatcher extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(SpecialReportDispatcher.class);

    /** Number of phase-report entries each player has already been sent, keyed by player id. */
    private final Map<Integer, Integer> sentReportCountByPlayer = new HashMap<>();

    SpecialReportDispatcher(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Sends the given player every phase report added since they last received one. Does nothing when there is
     * nothing new for them, which spares the client a redundant packet and a repeated notification.
     *
     * @param playerId the player to bring up to date
     */
    void sendNewReportsTo(int playerId) {
        Vector<Report> phaseReport = gameManager.getMainPhaseReport();
        int alreadySent = sentReportCountByPlayer.getOrDefault(playerId, 0);
        if (alreadySent > phaseReport.size()) {
            // The phase report was cleared since this player's last send, so the mark no longer means anything.
            // Correct the stored mark now (not just the local copy) so that if we bail out at the "nothing new"
            // check below - which happens when the report is currently empty - a later call self-heals instead
            // of re-detecting and re-logging the same stale mark every time.
            LOGGER.debug("[SpecialReport] player {}: mark {} exceeds report size {}, resending from the start",
                  playerId, alreadySent, phaseReport.size());
            alreadySent = 0;
            sentReportCountByPlayer.put(playerId, 0);
        }
        if (alreadySent == phaseReport.size()) {
            LOGGER.debug("[SpecialReport] player {}: nothing new since last send, skipping packet", playerId);
            return;
        }
        Vector<Report> newReports = new Vector<>(phaseReport.subList(alreadySent, phaseReport.size()));
        sentReportCountByPlayer.put(playerId, phaseReport.size());
        LOGGER.debug("[SpecialReport] player {}: sending {} new report(s) of {} in the phase",
              playerId, newReports.size(), phaseReport.size());
        gameManager.send(playerId, gameManager.createSpecialReportPacket(newReports));
    }

    /**
     * Brings each of the given players up to date. A player listed twice receives one packet, because the second
     * visit finds nothing new left to send.
     *
     * @param playerIds the player ids to bring up to date; repeats are harmless
     */
    void sendNewReportsTo(Iterable<Integer> playerIds) {
        for (Integer playerId : playerIds) {
            sendNewReportsTo(playerId);
        }
    }

    /** Forgets every player's mark. Called when the phase report itself is cleared. */
    void reset() {
        sentReportCountByPlayer.clear();
    }
}
