/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.autoResolve.acar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.Player;
import megamek.common.autoResolve.acar.action.Action;
import megamek.common.autoResolve.acar.action.MoraleCheckAction;
import megamek.common.autoResolve.acar.action.MoveAction;
import megamek.common.autoResolve.acar.action.RecoveringNerveAction;
import megamek.common.autoResolve.acar.action.WithdrawAction;
import megamek.common.autoResolve.acar.manager.ActionsProcessor;
import megamek.common.autoResolve.acar.manager.InitiativeHelper;
import megamek.common.autoResolve.acar.manager.PhaseEndManager;
import megamek.common.autoResolve.acar.manager.PhasePreparationManager;
import megamek.common.autoResolve.acar.manager.VictoryHelper;
import megamek.common.autoResolve.acar.phase.PhaseHandler;
import megamek.common.autoResolve.acar.report.HtmlGameLogger;
import megamek.common.autoResolve.acar.report.PublicReportEntry;
import megamek.common.autoResolve.component.Formation;
import megamek.common.autoResolve.event.AutoResolveConcludedEvent;
import megamek.common.board.BoardLocation;
import megamek.common.enums.GamePhase;
import megamek.common.game.IGame;
import megamek.common.interfaces.ReportEntry;
import megamek.common.net.packets.Packet;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import megamek.server.AbstractGameManager;
import megamek.server.Server;
import megamek.server.commands.ServerCommand;
import megamek.server.victory.VictoryResult;

public class SimulationManager extends AbstractGameManager {
    private static final MMLogger logger = MMLogger.create(SimulationManager.class);
    private final HtmlGameLogger gameLogger = HtmlGameLogger.create(PreferenceManager.getClientPreferences()
          .getAutoResolveGameLogFilename());

    private final List<ReportEntry> pendingReports = new ArrayList<>();
    private final List<PhaseHandler> phaseHandlers = new ArrayList<>();
    private final PhaseEndManager phaseEndManager;
    private final PhasePreparationManager phasePreparationManager;
    private final ActionsProcessor actionsProcessor;
    private final InitiativeHelper initiativeHelper;
    private final VictoryHelper victoryHelper;
    private final SimulationContext simulationContext;
    private final boolean suppressLog;

    public SimulationManager(SimulationContext simulationContext, boolean suppressLog) {
        this.simulationContext = simulationContext;
        this.suppressLog = suppressLog;
        this.phaseEndManager = new PhaseEndManager(this);
        this.phasePreparationManager = new PhasePreparationManager(this);
        this.actionsProcessor = new ActionsProcessor(this);
        this.initiativeHelper = new InitiativeHelper(this);
        this.victoryHelper = new VictoryHelper(this);
    }

    public AutoResolveConcludedEvent execute() {
        changePhase(GamePhase.STARTING_SCENARIO);
        while (!simulationContext.getPhase().equals(GamePhase.VICTORY)) {
            changePhase(GamePhase.INITIATIVE);
        }
        return getConclusionEvent();
    }

    public void addPhaseHandler(PhaseHandler phaseHandler) {
        phaseHandlers.add(phaseHandler);
    }

    public HtmlGameLogger getGameLogger() {
        return gameLogger;
    }

    public AutoResolveConcludedEvent getConclusionEvent() {
        return new AutoResolveConcludedEvent(getGame(), getCurrentVictoryResult(), gameLogger.getLogFile());
    }

    public void setFormationAt(Formation formation, BoardLocation position) {
        getGame().setFormationAt(formation, position);
    }

    @Override
    protected void endCurrentPhase() {
        logger.debug("Ending phase {}", getGame().getPhase());
        phaseEndManager.managePhase();
    }

    @Override
    public void prepareForCurrentPhase() {
        logger.debug("Preparing phase {}", getGame().getPhase());
        phasePreparationManager.managePhase();
    }

    @Override
    public void executeCurrentPhase() {
        logger.debug("Executing phase {}", getGame().getPhase());
        phaseHandlers.forEach(PhaseHandler::execute);
        endCurrentPhase();
    }

    /**
     * Called at the beginning of certain phases to make every player ready.
     */
    public void resetPlayersDone() {
        for (Player player : getGame().getPlayersList()) {
            player.setDone(false);
        }
    }

    public void resetFormationsDone() {
        for (var formation : getGame().getActiveFormations()) {
            formation.setDone(false);
        }
    }

    public void resetFormations() {
        for (var formation : getGame().getActiveFormations()) {
            formation.reset();
        }
    }

    /**
     * Rolls initiative for all teams.
     */
    public void rollInitiative() {
        initiativeHelper.rollInitiativeForFormations(getGame().getActiveFormations());
    }


    public void resetInitiative() {
        initiativeHelper.resetInitiative();
    }

    /**
     * Returns the victory result.
     */
    public VictoryResult getCurrentVictoryResult() {
        return victoryHelper.getVictoryResult();
    }

    public boolean isVictory() {
        return getCurrentVictoryResult().isVictory();
    }

    @Override
    public SimulationContext getGame() {
        return simulationContext;
    }

    @Override
    public void setGame(IGame game) {
        throw new UnsupportedOperationException("Cannot set game in SimulationManager");
    }

    public InitiativeHelper getInitiativeHelper() {
        return initiativeHelper;
    }

    public ActionsProcessor getActionsProcessor() {
        return actionsProcessor;
    }

    @SuppressWarnings("unused")
    public void addMovement(MoveAction moveAction, Formation activeFormation) {
        getGame().addAction(moveAction);
        activeFormation.setDone(true);
    }

    public void addMoraleCheck(MoraleCheckAction acsMoraleCheckAction, Formation formation) {
        getGame().addAction(acsMoraleCheckAction);
        formation.setDone(true);
    }

    public void addAttack(List<Action> actions, Formation formation) {
        actions.forEach(getGame()::addAction);
        formation.setDone(true);
    }

    public void addNerveRecovery(RecoveringNerveAction recoveringNerveAction) {
        getGame().addAction(recoveringNerveAction);
    }

    public void addWithdraw(WithdrawAction acsWithdrawAction) {
        getGame().addAction(acsWithdrawAction);
    }

    public void flushPendingReports() {
        if (!isLogSuppressed()) {
            pendingReports.forEach(r -> gameLogger.addRaw(r.text()));
        }
        pendingReports.clear();
    }

    @Override
    protected void sendPhaseChange() {
        // DO NOTHING
    }

    @Override
    public List<ServerCommand> getCommandList(Server server) {
        return Collections.emptyList();
    }

    @Override
    public void addReport(ReportEntry r) {
        if (isLogSuppressed()) {
            return;
        }
        if (r instanceof PublicReportEntry publicReportEntry) {
            pendingReports.add(publicReportEntry);
        } else {
            pendingReports.add(new PublicReportEntry("acar.initiative.freePlaceholder").add(r.text()));
        }
    }

    @Override
    public void calculatePlayerInitialCounts() {
        for (Player player : getGame().getPlayersList()) {
            player.setInitialEntityCount(Math.toIntExact(getGame().getActiveFormations(player)
                  .stream()
                  .filter(entity -> !entity.isRouted())
                  .count()));
            getGame().getActiveFormations(player)
                  .stream()
                  .map(Formation::getPointValue)
                  .reduce(Integer::sum)
                  .ifPresent(player::setInitialBV);
        }
    }

    public boolean isLogSuppressed() {
        return suppressLog;
    }

    @Override
    public void requestTeamChangeForPlayer(int teamID, Player player) {
        // DO NOTHING
    }

    @Override
    public void removeAllEntitiesOwnedBy(Player player) {
        // DO NOTHING
    }

    @Override
    public void resetGame() {
        // DO NOTHING
    }

    // not to be implemented methods
    @Override
    public void disconnect(Player player) {
        // DO NOTHING
    }

    @Override
    public void sendCurrentInfo(int connId) {
        // DO NOTHING
    }

    @Override
    public void handleCfrPacket(Server.ReceivedPacket rp) {
        // DO NOTHING
    }

    @Override
    public void requestGameMaster(Player player) {
        // DO NOTHING
    }

    @Override
    public void send(Packet packet) {
        // DO NOTHING
    }

    @Override
    public void send(int connId, Packet p) {
        // DO NOTHING
    }
}
