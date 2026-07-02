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

package megamek.server.victory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.logging.MMLogger;

/**
 * Holds the successful scans of the Sensor Check mission (Standard Missions, Objectives): each scanning unit banks
 * its successful scans of enemy units here during play, and the banked scans are converted into Victory Points by
 * {@link megamek.server.totalWarfare.TWGameManager} objective resolution when the scanner exfiltrates (flees the
 * board from round 5 on). A unit can bank each enemy unit only once.
 *
 * <P>Like {@link VictoryPointTracker}, the tally is stored in the game's victory context under
 * {@link #VICTORY_CONTEXT_KEY}, so it is part of savegames and survives save/load.</P>
 */
public class ScanTally implements Serializable {

    /** The key under which the tally is stored in the game's victory context map. */
    public static final String VICTORY_CONTEXT_KEY = "ScanTally";

    private static final MMLogger LOGGER = MMLogger.create(ScanTally.class);
    private static final long serialVersionUID = 1L;

    private final Map<Integer, Set<Integer>> scannedTargetsByScanner = new HashMap<>();
    private final Set<Integer> exfiltrationProcessedScanners = new HashSet<>();

    /**
     * Records a successful scan. A scanner banks each target only once; repeat scans of the same target are ignored.
     *
     * @param scannerId The entity ID of the scanning unit
     * @param targetId  The entity ID of the scanned enemy unit
     *
     * @return {@code true} if this is a new scan for the scanner, {@code false} if it had already scanned the target
     */
    public boolean recordScan(int scannerId, int targetId) {
        boolean newScan = scannedTargetsByScanner.computeIfAbsent(scannerId, key -> new HashSet<>()).add(targetId);
        if (newScan) {
            LOGGER.debug("[Scan] Scanner {} banked a successful scan of target {}; scans banked: {}",
                  scannerId, targetId, getScanCount(scannerId));
        }
        return newScan;
    }

    /**
     * @param scannerId The entity ID of a scanning unit
     * @param targetId  The entity ID of a potential target
     *
     * @return {@code true} if the scanner has already banked a successful scan of the target
     */
    public boolean hasScanned(int scannerId, int targetId) {
        return scannedTargetsByScanner.getOrDefault(scannerId, Set.of()).contains(targetId);
    }

    /**
     * @param scannerId The entity ID of a scanning unit
     *
     * @return The number of successful scans the scanner has banked
     */
    public int getScanCount(int scannerId) {
        return scannedTargetsByScanner.getOrDefault(scannerId, Set.of()).size();
    }

    /**
     * Marks a scanner's exfiltration as processed so its banked scans are converted (or forfeited) exactly once.
     *
     * @param scannerId The entity ID of the scanning unit
     */
    public void markExfiltrationProcessed(int scannerId) {
        exfiltrationProcessedScanners.add(scannerId);
    }

    /**
     * @param scannerId The entity ID of a scanning unit
     *
     * @return {@code true} if the scanner's exfiltration has already been processed
     */
    public boolean isExfiltrationProcessed(int scannerId) {
        return exfiltrationProcessedScanners.contains(scannerId);
    }

    /**
     * Retrieves the scan tally of the given game, creating and storing it in the game's victory context if it does
     * not exist yet.
     *
     * @param game The game
     *
     * @return The game's scan tally, never {@code null}
     */
    public static ScanTally getTally(Game game) {
        Map<String, Object> victoryContext = game.getVictoryContext();
        if (victoryContext == null) {
            LOGGER.debug("[Scan] Victory context not yet initialized; creating it to hold the scan tally");
            game.setVictoryContext(new HashMap<>());
            victoryContext = game.getVictoryContext();
        }
        if (victoryContext.get(VICTORY_CONTEXT_KEY) instanceof ScanTally tally) {
            return tally;
        }
        ScanTally tally = new ScanTally();
        victoryContext.put(VICTORY_CONTEXT_KEY, tally);
        return tally;
    }

    /**
     * Looks up the scan tally in the given victory context without creating one.
     *
     * @param victoryContext The victory context to search, or {@code null} when no context exists
     *
     * @return The tally stored in the context, or {@code null} if the context is {@code null} or holds no tally
     */
    public static @Nullable ScanTally findTally(@Nullable Map<String, Object> victoryContext) {
        if ((victoryContext != null)
              && (victoryContext.get(VICTORY_CONTEXT_KEY) instanceof ScanTally tally)) {
            return tally;
        }
        return null;
    }
}
