/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.alphaStrike;

import java.util.List;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.enums.GamePhase;
import megamek.common.game.AbstractGame;
import megamek.common.game.GameTurn;
import megamek.common.game.InGameObject;
import megamek.common.interfaces.ReportEntry;
import megamek.common.options.GameOptions;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;

/**
 * This is an Alpha Strike game object that holds all game information. This is intentionally a stub and currently only
 * intended to see how to work with MM's interfaces. In the future, it could be completed to support AS games.
 */
public class ASGame extends AbstractGame {

    private final GamePhase phase = GamePhase.UNKNOWN;
    private Board board = new Board();

    @Override
    public GameTurn getTurn() {
        return null;
    }

    @Override
    public List<GameTurn> getTurnsList() {
        return List.of();
    }

    @Override
    public IGameOptions getOptions() {
        return null;
    }

    @Override
    public GamePhase getPhase() {
        return null;
    }

    @Override
    public void setPhase(GamePhase phase) {

    }

    @Override
    public void setLastPhase(GamePhase lastPhase) {
    }

    @Override
    public boolean isForceVictory() {
        return false;
    }

    @Override
    public void addPlayer(int id, Player player) {
        super.addPlayer(id, player);
        player.setGame(this);
        setupTeams();

        if ((player.isBot()) && (!player.getSingleBlind())) {
            boolean sbb = getOptions().booleanOption(OptionsConstants.ADVANCED_SINGLE_BLIND_BOTS);
            boolean db = getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
            player.setSingleBlind(sbb && db);
        }
    }

    @Override
    public boolean isCurrentPhasePlayable() {
        return switch (phase) {
            case INITIATIVE, END, TARGETING -> false;
            case PHYSICAL,
                 OFFBOARD,
                 OFFBOARD_REPORT,
                 DEPLOYMENT,
                 PREMOVEMENT,
                 MOVEMENT, PRE_FIRING,
                 FIRING,
                 DEPLOY_MINEFIELDS,
                 SET_ARTILLERY_AUTO_HIT_HEXES -> hasMoreTurns();
            default -> true;
        };
    }

    @Override
    public void setPlayer(int id, Player player) {

    }

    @Override
    public void removePlayer(int id) {

    }

    @Override
    public void setupTeams() {

    }

    @Override
    public void replaceUnits(List<InGameObject> units) {

    }

    @Override
    public List<InGameObject> getGraveyard() {
        return null;
    }

    @Override
    public ReportEntry getNewReport(int messageId) {
        return new Report(messageId);
    }

    private boolean isSupportedUnitType(InGameObject object) {
        return object instanceof AlphaStrikeElement;
    }

    public void setOptions(GameOptions options) {
    }

    @Override
    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }
}
