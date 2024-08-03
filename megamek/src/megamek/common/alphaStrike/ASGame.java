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
package megamek.common.alphaStrike;

import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

import java.util.List;

/**
 * This is an Alpha Strike game object that holds all game information. This is intentionally a stub and
 * currently only intended to see how to work with MM's interfaces. In the future, it could be completed
 * to support AS games.
 */
public class ASGame extends AbstractGame {

    private GameOptions options = new GameOptions();
    private GamePhase phase = GamePhase.UNKNOWN;
    private GamePhase lastPhase = GamePhase.UNKNOWN;
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
    public GameOptions getOptions() {
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
        this.lastPhase = lastPhase;
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
        switch (phase) {
            case INITIATIVE:
            case END:
            case TARGETING:
                return false;
            case PHYSICAL:
            case OFFBOARD:
            case OFFBOARD_REPORT:
            case DEPLOYMENT:
            case PREMOVEMENT:
            case MOVEMENT:
            case PREFIRING:
            case FIRING:
            case DEPLOY_MINEFIELDS:
            case SET_ARTILLERY_AUTOHIT_HEXES:
                return hasMoreTurns();
            default:
                return true;
        }
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

}
