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

package megamek.common.strategicBattleSystems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

import megamek.client.ui.clientGUI.sbf.SelectDirection;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.SBFFullGameReport;
import megamek.common.Team;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.event.UnitChangedGameEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.AbstractGame;
import megamek.common.game.InGameObject;
import megamek.common.interfaces.ClientOnly;
import megamek.common.interfaces.PlanetaryConditionsUsing;
import megamek.common.interfaces.ReportEntry;
import megamek.common.interfaces.ServerOnly;
import megamek.common.options.OptionsConstants;
import megamek.common.options.SBFRuleOptions;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.BTObject;
import megamek.logging.MMLogger;
import megamek.server.sbf.SBFActionHandler;

/**
 * This is an SBF game's game object that holds all game information. As of 2024, this is under construction.
 */
public final class SBFGame extends AbstractGame implements PlanetaryConditionsUsing, SBFRuleOptionsUser {
    private static final MMLogger logger = MMLogger.create(SBFGame.class);

    private final SBFRuleOptions options = new SBFRuleOptions();
    private GamePhase phase = GamePhase.UNKNOWN;
    private GamePhase lastPhase = GamePhase.UNKNOWN;
    private final PlanetaryConditions planetaryConditions = new PlanetaryConditions();
    private final SBFFullGameReport gameReport = new SBFFullGameReport();
    private final List<SBFTurn> turnList = new ArrayList<>();
    private final SBFVisibilityHelper visibilityHelper = new SBFVisibilityHelper(this);
    private final List<SBFActionHandler> actionHandlers = new ArrayList<>();

    /**
     * Contains all units that have left the game by any means.
     */
    private final List<InGameObject> graveyard = new ArrayList<>();

    public SBFGame() {
        setBoard(0, new Board());
    }

    @Override
    public @Nullable SBFTurn getTurn() {
        if ((turnIndex < 0) || (turnIndex >= turnList.size())) {
            return null;
        } else {
            return turnList.get(turnIndex);
        }
    }

    @Override
    public SBFRuleOptions getOptions() {
        return options;
    }

    @Override
    public GamePhase getPhase() {
        return phase;
    }

    public GamePhase getLastPhase() {
        return lastPhase;
    }

    @Override
    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    @Override
    public void receivePhase(GamePhase phase) {
        GamePhase oldPhase = this.phase;
        setPhase(phase);
        fireGameEvent(new GamePhaseChangeEvent(this, oldPhase, phase));
    }

    @Override
    public boolean isForceVictory() { // TODO This should not be part of IGame! too specific
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
        return switch (phase) {
            case INITIATIVE, END, TARGETING, PHYSICAL, OFFBOARD, OFFBOARD_REPORT, SBF_DETECTION, SBF_DETECTION_REPORT ->
                  false;
            case DEPLOYMENT,
                 PREMOVEMENT,
                 MOVEMENT,
                 PRE_FIRING,
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
        Vector<Team> initTeams = new Vector<>();
        boolean useTeamInit = getOptions().booleanOption(OptionsConstants.BASE_TEAM_INITIATIVE);

        // Get all NO_TEAM players. If team_initiative is false, all
        // players are on their own teams for initiative purposes.
        for (Player player : getPlayersList()) {
            // Ignore players not on a team
            if (player.getTeam() == Player.TEAM_UNASSIGNED) {
                continue;
            }
            if (!useTeamInit || (player.getTeam() == Player.TEAM_NONE)) {
                Team new_team = new Team(Player.TEAM_NONE);
                new_team.addPlayer(player);
                initTeams.addElement(new_team);
            }
        }

        if (useTeamInit) {
            // Now, go through all the teams, and add the appropriate player
            for (int t = Player.TEAM_NONE + 1; t < Player.TEAM_NAMES.length; t++) {
                Team new_team = null;
                for (Player player : getPlayersList()) {
                    if (player.getTeam() == t) {
                        if (new_team == null) {
                            new_team = new Team(t);
                        }
                        new_team.addPlayer(player);
                    }
                }

                if (new_team != null) {
                    initTeams.addElement(new_team);
                }
            }
        }

        // May need to copy state over from previous teams, such as initiative
        if (!getPhase().isLounge()) {
            for (Team newTeam : initTeams) {
                for (Team oldTeam : teams) {
                    if (newTeam.equals(oldTeam)) {
                        newTeam.setInitiative(oldTeam.getInitiative());
                    }
                }
            }
        }

        // Carry over faction settings
        for (Team newTeam : initTeams) {
            for (Team oldTeam : teams) {
                if (newTeam.equals(oldTeam)) {
                    newTeam.setFaction(oldTeam.getFaction());
                }
            }
        }

        teams.clear();
        teams.addAll(initTeams);
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
            logger.error("Can't set the planetary conditions to null!");
        } else {
            planetaryConditions.alterConditions(conditions);
        }
    }

    public void addUnit(InGameObject unit) { // This is a server-side method!
        if (!isSupportedUnitType(unit)) {
            logger.error("Tried to add unsupported object [{}] to the game!", unit);
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

    /**
     * Adds or overwrites the received unit in the game's unit list.
     *
     * @param unit The new or changed unit
     */
    @ClientOnly
    public void receiveUnit(InGameObject unit) {
        InGameObject oldUnit = inGameObjects.put(unit.getId(), unit);
        fireGameEvent(new UnitChangedGameEvent(this, oldUnit, unit));
    }

    private boolean isSupportedUnitType(InGameObject object) {
        return object instanceof SBFFormation || object instanceof AlphaStrikeElement || object instanceof SBFUnit;
    }

    @Override
    public void setLastPhase(GamePhase lastPhase) {
        this.lastPhase = lastPhase;
    }

    /**
     * Adds the given reports this game's reports.
     *
     * @param reports the new reports to add
     */
    public void addReports(List<SBFReportEntry> reports) {
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
    public void replaceAllReports(Map<Integer, List<SBFReportEntry>> newReports) {
        gameReport.replaceAllReports(newReports);
    }

    /**
     * Sets the current list of turns to the given one, replacing any currently present turns.
     *
     * @param newTurns The new list of turns to use
     */
    public void setTurns(List<SBFTurn> newTurns) {
        turnList.clear();
        turnList.addAll(newTurns);
    }

    /**
     * Returns the current list of turns. The returned list is unmodifiable but not a deep copy. If you're not the
     * SBFGameManager, don't even think about changing any of the turns.
     */
    @Override
    public List<SBFTurn> getTurnsList() {
        return Collections.unmodifiableList(turnList);
    }

    @Override
    public List<InGameObject> getGraveyard() {
        return graveyard;
    }

    /**
     *
     */
    @ClientOnly
    public void setUnitList(List<InGameObject> units) {
        inGameObjects.clear();
        for (InGameObject unit : units) {
            int id = unit.getId();
            inGameObjects.put(id, unit);
        }
        fireGameEvent(new GameEntityChangeEvent(this, null));
    }

    public void setGraveyard(List<InGameObject> graveyard) {
        this.graveyard.clear();
        this.graveyard.addAll(graveyard);
    }

    @ServerOnly
    public SBFVisibilityHelper visibilityHelper() {
        return visibilityHelper;
    }

    @ServerOnly
    public boolean isVisible(int viewingPlayer, int formationID) {
        return visibilityHelper.isVisible(viewingPlayer, formationID);
    }

    @ServerOnly
    public List<InGameObject> getFullyVisibleUnits(Player viewingPlayer) {
        return getInGameObjects().stream().filter(unit -> isVisible(viewingPlayer.getId(), unit.getId())).toList();
    }

    public SBFVisibilityStatus getVisibility(Player player, int unitId) {
        return visibilityHelper.getVisibility(player.getId(), unitId);
    }

    /**
     * Advances the turn index and returns the then-current turn.
     *
     * @return The current turn (after advancing the turn index)
     */
    public SBFTurn changeToNextTurn() {
        turnIndex++;
        return getTurn();
    }

    public boolean hasEligibleFormation(SBFFormationTurn turn) {
        // TODO: called from turn and asks back in turn... circular, improve this
        return (turn != null) && getActiveFormations().stream().anyMatch(f -> turn.isValidEntity(f, this));
    }

    /**
     * Returns the list of formations that are in the game's InGameObject list, i.e. that aren't destroyed or otherwise
     * removed from play.
     *
     * @return The currently active formations
     */
    public List<SBFFormation> getActiveFormations() {
        return inGameObjects.values().stream()
              .filter(u -> u instanceof SBFFormation)
              .map(u -> (SBFFormation) u)
              .collect(Collectors.toList());
    }

    /**
     * Returns the list of formations that are in the game's InGameObject list, i.e. that aren't destroyed or otherwise
     * removed from play.
     *
     * @return The currently active formations
     */
    public List<SBFFormation> getActiveFormationsAt(BoardLocation location) {
        return getActiveFormations().stream()
              .filter(f -> f.getPosition() != null)
              .filter(f -> f.getPosition().equals(location))
              .collect(Collectors.toList());
        // TODO: cache when receiving units at the Client
    }

    public boolean isHostileActiveFormationAt(BoardLocation location, SBFFormation formation) {
        Player owner = getPlayer(formation.getOwnerId());
        return getActiveFormationsAt(location).stream()
              .map(f -> getPlayer(f.getOwnerId()))
              .anyMatch(owner::isEnemyOf);
    }

    public boolean areHostile(SBFFormation formation, Player player) {
        return player.isEnemyOf(getPlayer(formation.getOwnerId()));
    }

    // check current turn, phase, formation
    private boolean isEligibleForAction(SBFFormation formation) {
        return (getTurn() instanceof SBFFormationTurn)
              && getTurn().isValidEntity(formation, this);
    }

    /**
     * @return the first formation in the list of formations that is alive and eligible for the current game phase.
     */
    public Optional<SBFFormation> getNextEligibleFormation() {
        return getNextEligibleFormation(BTObject.NONE);
    }

    /**
     * @return the preceding formation in the list of formations that is alive and eligible for the current game phase.
     */
    public Optional<SBFFormation> getPreviousEligibleFormation() {
        return getPreviousEligibleFormation(BTObject.NONE);
    }

    /**
     * @return the next in the list of formations that is alive and eligible for the current game phase, counting from
     *       the given current formation id. If no matching formation can be found for the given id, returns the first
     *       eligible formation in the list of formations that is alive and eligible.
     */
    public Optional<SBFFormation> getNextEligibleFormation(int currentFormationID) {
        return getEligibleFormationImpl(currentFormationID, phase, SelectDirection.NEXT_UNIT);
    }

    /**
     * @return the previous in the list of formations that is alive and eligible for the current game phase, counting
     *       from the given current formation id. If no matching formation can be found for the given id, returns the
     *       last eligible formation from the list of formations that is alive and eligible.
     */
    public Optional<SBFFormation> getPreviousEligibleFormation(int currentFormationID) {
        return getEligibleFormationImpl(currentFormationID, phase, SelectDirection.PREVIOUS_UNIT);
    }

    /**
     * Based on the given search direction, returns the formation that precedes or follows the given formation ID in the
     * list of active formations eligible for action in the given game phase.
     *
     * @param currentFormationID the start point of the formation search. Need not match an actual formation
     * @param phase              the phase to check
     * @param direction          the selection to seek the next or previous formation
     *
     * @return the formation that precedes or follows the given formation ID, if one can be found
     */
    private Optional<SBFFormation> getEligibleFormationImpl(int currentFormationID, GamePhase phase,
          SelectDirection direction) {
        List<SBFFormation> eligibleFormations = getActiveFormations().stream()
              .filter(this::isEligibleForAction)
              .toList();
        if (eligibleFormations.isEmpty()) {
            return Optional.empty();
        } else {
            Optional<SBFFormation> currentFormation = getFormation(currentFormationID);
            int index = currentFormation.map(eligibleFormations::indexOf).orElse(-1);
            if (index == -1) {
                // when no current unit is found, the next unit is the first, the previous unit
                // is the last
                index = direction.isNextUnit() ? 0 : eligibleFormations.size() - 1;
            } else {
                // must add the list size to safely get the previous unit because -1 % 5 == -1
                // (not 4)
                index += eligibleFormations.size() + (direction.isNextUnit() ? 1 : -1);
                index %= eligibleFormations.size();
            }
            return Optional.ofNullable(eligibleFormations.get(index));
        }
    }

    /**
     * Returns the formation of the given ID, if one can be found.
     *
     * @param formationID the ID to look for
     *
     * @return The formation or an empty Optional
     */
    public Optional<SBFFormation> getFormation(int formationID) {
        Optional<InGameObject> unit = getInGameObject(formationID);
        if (unit.isPresent() && unit.get() instanceof SBFFormation) {
            return Optional.of((SBFFormation) unit.get());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean hasMoreTurns() {
        return getTurnsList().size() > turnIndex + 1;
    }

    /**
     * Returns the Player that has to act in the given turn. The result should rarely be empty.
     *
     * @param turn The SBFTurn to check
     *
     * @return The Player whose turn it is
     */
    public Optional<Player> getPlayerFor(SBFTurn turn) {
        return Optional.ofNullable(getPlayer(turn.playerId()));
    }

    /**
     * Sets the current turn index
     *
     * @param turnIndex    The new turn index.
     * @param prevPlayerId The ID of the player who triggered the turn index change.
     */
    public void setTurnIndex(int turnIndex, int prevPlayerId) {
        setTurnIndex(turnIndex);
        SBFTurn currentTurn = getTurn();
        if (currentTurn != null) {
            fireGameEvent(new GameTurnChangeEvent(this, getPlayer(currentTurn.playerId()), prevPlayerId));
        }
    }

    public boolean onSameBoard(SBFFormation unit1, SBFFormation unit2) {
        return (unit1.getPosition() != null) && unit1.getPosition().isSameBoardAs(unit2.getPosition());
    }

    public void forget(int unitId) {
        inGameObjects.remove(unitId);
    }

    public void addActionHandler(SBFActionHandler handler) {
        if (actionHandlers.contains(handler)) {
            logger.error("Tried to re-add action handler {}!", handler);
        } else {
            actionHandlers.add(handler);
        }
    }

    public void removeActionHandler(SBFActionHandler handler) {
        if (!actionHandlers.remove(handler)) {
            logger.error("Tried to remove non-existent action handler {}!", handler);
        }
    }

    /**
     * @return The currently active action handlers.
     */
    public List<SBFActionHandler> getActionHandlers() {
        return actionHandlers;
    }
}
