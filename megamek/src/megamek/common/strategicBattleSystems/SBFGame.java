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
package megamek.common.strategicBattleSystems;

import megamek.common.*;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameEvent;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import org.apache.logging.log4j.LogManager;

import java.util.Collections;
import java.util.List;

/**
 * This is an SBF game's game object that holds all game information. As of 2024, this is under construction.
 */
public class SBFGame extends AbstractGame implements PlanetaryConditionsUsing {

    private GameOptions options = new GameOptions(); //TODO: SBFGameOptions()
    private GamePhase phase = GamePhase.UNKNOWN; //TODO: SBFGamePhase - or possibly reuse Phase? very similar
    private Board board = new Board();
    private PlanetaryConditions planetaryConditions = new PlanetaryConditions();

    @Override
    public GameTurn getTurn() {
        return null;
    }

    @Override
    public GameOptions getOptions() {
        return options;
    }

    @Override
    public GamePhase getPhase() {
        return phase;
    }

    @Override
    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    @Override
    public void fireGameEvent(GameEvent event) {

    }

    @Override
    public boolean isForceVictory() { //TODO This should not be part of IGame! too specific
        return false;
    }

    @Override
    public void addPlayer(int id, Player player) { // Server / Client-side?
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
        return Collections.max(inGameObjects.keySet()) + 1;
    }

    @Override
    public void replaceUnits(List<InGameObject> units) {

    }

    @Override
    public void setBoard(Board board, int boardId) {
        this.board = board;
    }

    @Override
    public PlanetaryConditions getPlanetaryConditions() {
        return planetaryConditions;
    }

    @Override
    public void setPlanetaryConditions(@Nullable PlanetaryConditions conditions) {
        if (conditions == null) {
            LogManager.getLogger().error("Can't set the planetary conditions to null!");
        } else {
            planetaryConditions.alterConditions(conditions);
        }
    }

    public void addUnit(InGameObject unit) { // This is a server-side method!
        if (!isSupportedUnitType(unit)) {
            LogManager.getLogger().error("Tried to add unsupported object [" + unit + "] to the game!");
            return;
        }

        // Add this unit, ensuring that its id is unique
        int id = unit.getId();
        if (inGameObjects.containsKey(id)) {
            id = getNextEntityId();
            unit.setId(id);
        }
        inGameObjects.put(id, unit);
    }

    public void receiveUnit(InGameObject unit) { // This is a client-side method to set a unit sent by the server!
        inGameObjects.put(unit.getId(), unit);
    }

    private boolean isSupportedUnitType(InGameObject object) {
        return object instanceof SBFFormation || object instanceof AlphaStrikeElement || object instanceof SBFUnit;
    }
}
