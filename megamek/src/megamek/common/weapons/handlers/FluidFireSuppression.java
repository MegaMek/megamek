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
package megamek.common.weapons.handlers;

import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.rolls.Roll;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Shared resolution for a fire-suppressant fluid (Water or Coolant) attempting to put out the fire in a
 * targeted hex (TO:AUE pp.173-174). Flame-Retardant Foam douses fires instantly and so handles its own
 * resolution rather than rolling here.
 *
 * @author The MegaMek Team
 */
public final class FluidFireSuppression {

    private FluidFireSuppression() {
    }

    /**
     * Rolls 2D6 to put out the fire in a targeted hex: an ordinary fire is doused on {@code normalFireRoll}
     * or better, while an Inferno-fuelled fire requires a 12 (TO:AUE pp.173-174). On success the fire (and
     * any inferno / flamer-fire markers) is removed and the hex change is broadcast.
     *
     * @param game           the game
     * @param gameManager    the game manager (used to broadcast the hex change)
     * @param target         the {@code TYPE_HEX_EXTINGUISH} target hex
     * @param subjectId      the firing weapon's subject id for reporting
     * @param normalFireRoll the 2D6 target number to douse an ordinary fire (Water 3, Coolant 4)
     * @param vPhaseReport   the phase report to append to
     */
    public static void extinguishHex(Game game, TWGameManager gameManager, Targetable target, int subjectId,
          int normalFireRoll, Vector<Report> vPhaseReport) {
        int boardId = target.getBoardId();
        Coords coords = target.getPosition();
        Board board = game.getBoard(boardId);
        Hex hex = (board == null) ? null : board.getHex(coords);
        if (hex == null) {
            return;
        }

        int neededRoll = board.isInfernoBurning(coords) ? 12 : normalFireRoll;
        Roll diceRoll = Compute.rollD6(2);
        Report attempt = new Report(3389);
        attempt.subject = subjectId;
        attempt.add(coords.getBoardNum());
        attempt.add(diceRoll);
        attempt.add(neededRoll);
        attempt.indent(2);
        vPhaseReport.add(attempt);

        if (diceRoll.getIntValue() >= neededRoll) {
            hex.removeTerrain(Terrains.FIRE);
            hex.resetFireTurn();
            gameManager.sendChangedHex(coords, boardId);
            board.removeInfernoFrom(coords);
            board.removeFlamerStartedFire(coords);

            Report extinguished = new Report(3540);
            extinguished.subject = subjectId;
            extinguished.add(coords.getBoardNum());
            extinguished.indent(3);
            vPhaseReport.add(extinguished);
        }
    }
}
