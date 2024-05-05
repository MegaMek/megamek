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
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.Map;

/**
 * This is an SBF game's game object that holds all game information. As of 2024, this is under construction.
 */
public final class SBFGame extends AbstractGame implements PlanetaryConditionsUsing {

    private final GameOptions options = new GameOptions(); //TODO: SBFGameOptions()
    private GamePhase phase = GamePhase.UNKNOWN;
    private GamePhase lastPhase = GamePhase.UNKNOWN;
    private final PlanetaryConditions planetaryConditions = new PlanetaryConditions();
    private final SBFFullGameReport gameReport = new SBFFullGameReport();
    private final List<GameTurn> turnList = new ArrayList<>();


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
    public boolean isCurrentPhasePlayable() {
        switch (phase) {
            case INITIATIVE:
            case END:
            case TARGETING:
            case PHYSICAL:
            case OFFBOARD:
            case OFFBOARD_REPORT:
                return false;
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
    public int getNextEntityId() {
        return Collections.max(inGameObjects.keySet()) + 1;
    }

    @Override
    public void replaceUnits(List<InGameObject> units) {

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
            LogManager.getLogger().error("Tried to add unsupported object [{}] to the game!", unit);
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

    public void setLastPhase(GamePhase lastPhase) {
        this.lastPhase = lastPhase;
    }

    /**
     * Adds the given reports this game's reports.
     *
     * @param reports the new reports to add
     */
    public void addReports(List<Report> reports) {
        gameReport.add(getCurrentRound(), reports);
    }

    @Override
    public ReportEntry getNewReport(int messageId) {
        return new Report(messageId);
    }

    public SBFFullGameReport getGameReport() {
        return gameReport;
    }

    /**
     * Replaces this game's entire reports with the given reports.
     *
     * @param newReports The new reports to keep as this game's reports
     */
    public void replaceAllReports(Map<Integer, List<Report>> newReports) {
        gameReport.replaceAllReports(newReports);
    }

    public void clearTurns() {
        turnList.clear();
    }

    /**
     * Sets the current turn vector
     */
    public void setTurns(List<GameTurn> newTurns) {
        turnList.clear();
        turnList.addAll(newTurns);
    }

    /**
     * Returns the current list of turns. The returned list is unmodifiable but not a deep copy. If you're
     * not the SBFGameManager, don't even think about changing any of the turns.
     */
    public List<GameTurn> getTurnsList() {
        return Collections.unmodifiableList(turnList);
    }

    /**
     * Changes to the next turn, returning it.
     */
    public GameTurn changeToNextTurn() {
        turnIndex++;
        return getTurn();
    }
}
