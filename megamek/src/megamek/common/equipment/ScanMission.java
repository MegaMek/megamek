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
package megamek.common.equipment;

import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.ObjectiveScoringScheme.SchemePreset;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rules.RulesScanning;
import megamek.common.rules.tacops.TacOpsScanning;
import megamek.common.units.Entity;

/**
 * Whether scanning is part of this game, and whether a unit may order a scan. Shared by the client, which shows the
 * Scan order only when it can be given, and the server, which grants a pre-End declarations turn to every unit that
 * could give one (Objectives series, part 4).
 */
public final class ScanMission {

    private ScanMission() {}

    /**
     * Scanning is in play when objectives are on and there is something a scan could read: a Scan point on the
     * board, or the Sensor Check mission, in which enemy units are the targets. Without either, a scan can only ever
     * find nothing of interest, so no turn is spent on it.
     *
     * @param game the game, or {@code null} off-game
     *
     * @return {@code true} when a unit may be given a Scan order this game
     */
    public static boolean isInPlay(@Nullable Game game) {
        if ((game == null) || !game.getOptions().booleanOption(OptionsConstants.VICTORY_USE_OBJECTIVES)) {
            return false;
        }
        if (game.getOptions().booleanOption(OptionsConstants.VICTORY_USE_SENSOR_CHECK)) {
            return true;
        }
        for (List<ICarryable> hexObjects : game.getGroundObjects().values()) {
            for (ICarryable groundObject : hexObjects) {
                boolean isScanPoint = (groundObject instanceof ObjectiveMarker marker)
                      && (marker.getScoringScheme().getPreset() == SchemePreset.SCAN);
                if (isScanPoint) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The optional TacOps scanning rules, held once because they carry no state. */
    private static final TacOpsScanning TAC_OPS_SCANNING = new TacOpsScanning();

    /**
     * The scanning rules in force. The Core Rulebook's mission scanning check (p. 233) is the baseline in any game
     * with objectives; a game that switches on the optional TacOps: Advanced Rules scanning rule (p. 187) uses that
     * instead, where sensors read a target without a roll and an active probe is answered by a 2D6 roll of 8.
     *
     * @param game the game, or {@code null} before a unit has one
     *
     * @return the rules a scan is resolved by
     */
    public static RulesScanning scanningRules(@Nullable Game game) {
        boolean isTacOpsScanning = (game != null)
              && game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_SCANNING);
        return isTacOpsScanning ? TAC_OPS_SCANNING : Game.rulesManager.getRulesScanning();
    }

    /**
     * @param unit the unit that might scan
     *
     * @return {@code true} when scanning is in play and the unit is on the board, alive, not being carried, and able
     *       to scan under the game's ruleset - the reasons a scan could be refused, aerospace or wrecked sensors, are
     *       the ruleset's
     */
    public static boolean canOrderScan(Entity unit) {
        Game game = unit.getGame();
        if (!isInPlay(game)) {
            return false;
        }
        boolean isOnTheBoard = unit.isDeployed() && (unit.getPosition() != null) && !unit.isOffBoard()
              && !unit.isDestroyed() && (unit.getTransportId() == Entity.NONE);
        if (!isOnTheBoard) {
            return false;
        }
        return scanningRules(unit.getGame()).scanningRange(unit, null) > 0;
    }
}
