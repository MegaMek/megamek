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
import megamek.common.event.GameEvent;
import megamek.common.options.GameOptions;

import java.util.List;

/**
 * This is an Alpha Strike game object that holds all game information. This is intentionally a stub and
 * currently only intended to see how to work with MM's interfaces. In the future, it could be completed
 * to support AS games.
 */
public class ASGame extends AbstractGame {

    private GameOptions options = new GameOptions();
    private GamePhase phase = GamePhase.UNKNOWN;
    private Board board = new Board();

    @Override
    public GameTurn getTurn() {
        return null;
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
    public boolean isForceVictory() {
        return false;
    }

    @Override
    public void addPlayer(int id, Player player) {

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
    public int getNextEntityId() {
        return 0;
    }

    @Override
    public void replaceUnits(List<InGameObject> units) {

    }

}
