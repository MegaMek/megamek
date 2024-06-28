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
package megamek.server;

import megamek.common.*;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.server.commands.ServerCommand;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * This class manages an SBF game on the server side. As of 2024, this is under construction.
 */
public final class SBFGameManager extends AbstractGameManager {

    private SBFGame game;
    private final List<Report> pendingReports = new ArrayList<>();
    private final SBFPhaseEndManager phaseEndManager = new SBFPhaseEndManager(this);
    private final SBFPhasePreparationManager phasePreparationManager = new SBFPhasePreparationManager(this);

    @Override
    public SBFGame getGame() {
        return game;
    }

    @Override
    public void setGame(IGame g) {
        if (!(g instanceof SBFGame)) {
            LogManager.getLogger().fatal("Attempted to set game to incorrect class.");
            return;
        }
        game = (SBFGame) g;
    }

    @Override
    public void resetGame() { }

    @Override
    public void disconnect(Player player) { }

    @Override
    public void removeAllEntitiesOwnedBy(Player player) { }

    @Override
    public void handleCfrPacket(Server.ReceivedPacket rp) { }

    @Override
    public void requestGameMaster(Player player) { }

    @Override
    public void requestTeamChange(int teamId, Player player) { }

    @Override
    public List<ServerCommand> getCommandList(Server server) {
        return Collections.emptyList();
    }

    @Override
    public void addReport(ReportEntry r) {
        pendingReports.add((Report) r);
    }

    @Override
    public void calculatePlayerInitialCounts() { }

    @Override
    public void sendCurrentInfo(int connId) {
        send(connId, packetHelper.createGameSettingsPacket());

        Player player = getGame().getPlayer(connId);
        if (null != player) {
            send(connId, new Packet(PacketCommand.SENDING_MINEFIELDS, player.getMinefields()));

            if (getGame().getPhase().isLounge()) {
//                send(connId, createMapSettingsPacket());
//                send(createMapSizesPacket());
                // LOUNGE triggers a Game.reset() on the client!
                // Send Entities *after* the Lounge Phase Change
                send(connId, packetHelper.createPhaseChangePacket());
//                if (doBlind()) {
//                    send(connId, createFilteredFullEntitiesPacket(player, null));
//                } else {
//                    send(connId, createFullEntitiesPacket());
//                }
            } else {
                send(connId, packetHelper.createCurrentRoundNumberPacket());
                send(connId, packetHelper.createBoardsPacket());
                send(connId, createAllReportsPacket(player));
//
//                // Send entities *before* other phase changes.
//                if (doBlind()) {
//                    send(connId, createFilteredFullEntitiesPacket(player, null));
//                } else {
//                    send(connId, createFullEntitiesPacket());
//                }
//
//                setPlayerDone(player, getGame().getEntitiesOwnedBy(player) <= 0);
                send(connId, packetHelper.createPhaseChangePacket());
            }

            send(connId, packetHelper.createPlanetaryConditionsPacket());
//
            if (game.getPhase().isFiring() || game.getPhase().isTargeting()
                    || game.getPhase().isOffboard() || game.getPhase().isPhysical()) {
                // can't go above, need board to have been sent
//                send(connId, packetHelper.createAttackPacket(getGame().getActionsVector(), false));
//                send(connId, packetHelper.createAttackPacket(getGame().getChargesVector(), true));
//                send(connId, packetHelper.createAttackPacket(getGame().getRamsVector(), true));
//                send(connId, packetHelper.createAttackPacket(getGame().getTeleMissileAttacksVector(), true));
            }
//
            if (getGame().getPhase().usesTurns() && getGame().hasMoreTurns()) {
//                send(connId, createTurnVectorPacket());
//                send(connId, createTurnIndexPacket(connId));
            } else if (!getGame().getPhase().isLounge() && !getGame().getPhase().isStartingScenario()) {
                endCurrentPhase();
            }
//
//            send(connId, createArtilleryPacket(player));
//            send(connId, createFlarePacket());
//            send(connId, createSpecialHexDisplayPacket(connId));
//            send(connId, new Packet(PacketCommand.PRINCESS_SETTINGS, getGame().getBotSettings()));
        }
    }

    @Override
    protected void endCurrentPhase() {
        phaseEndManager.managePhase();
    }

    @Override
    protected void prepareForCurrentPhase() {
        phasePreparationManager.managePhase();
    }

    @Override
    protected void executeCurrentPhase() {
        switch (game.getPhase()) {
            case EXCHANGE:
                resetPlayersDone();
//                // Update initial BVs, as things may have been modified in lounge
//                for (Entity e : game.getEntitiesVector()) {
//                    e.setInitialBV(e.calculateBattleValue(false, false));
//                }
                calculatePlayerInitialCounts();
                game.setupTeams();
//                applyBoardSettings();
                game.getPlanetaryConditions().determineWind();
                send(packetHelper.createPlanetaryConditionsPacket());
                send(packetHelper.createBoardsPacket());
                game.setupDeployment();
//                game.setVictoryContext(new HashMap<>());
//                game.createVictoryConditions();
//                // some entities may need to be checked and updated
//                checkEntityExchange();
                break;
            case MOVEMENT:
                // write Movement Phase header to report
                addReport(new Report(2000, Report.PUBLIC));
                // intentional fall through
            case PREMOVEMENT:
            case SET_ARTILLERY_AUTOHIT_HEXES:
            case DEPLOY_MINEFIELDS:
            case DEPLOYMENT:
            case PREFIRING:
            case FIRING:
            case PHYSICAL:
            case TARGETING:
            case OFFBOARD:
                changeToNextTurn(-1);
                if (game.getOptions().booleanOption(OptionsConstants.BASE_PARANOID_AUTOSAVE)) {
                    autoSave();
                }
                break;
            default:
                break;
        }
    }

    /**
     * Called at the beginning of certain phases to make every player not ready.
     */
    private void resetPlayersDone() {
        //FIXME This is highly unclear why not in report but in victory
        if ((getGame().getPhase().isReport()) && (!getGame().getPhase().isVictory())) {
            return;
        }

        for (Player player : game.getPlayersList()) {
            setPlayerDone(player, false);
        }

        transmitAllPlayerDones();
    }

    private void setPlayerDone(Player player, boolean normalDone) {
        //FIXME This is highly specialized and very arcane!!
        if (getGame().getPhase().isReport()
                && getGame().getOptions().booleanOption(OptionsConstants.BASE_GM_CONTROLS_DONE_REPORT_PHASE)
                && getGame().getPlayersList().stream().filter(p -> p.isGameMaster()).count() > 0) {
            if (player.isGameMaster()) {
                player.setDone(false);
            } else {
                player.setDone(true);
            }
        } else {
            player.setDone(normalDone);
        }
    }

    /**
     * Called at the beginning of certain phases to make every active player not
     * ready.
     */
    void resetActivePlayersDone() {
        for (Player player : game.getPlayersList()) {
            //FIXME This is highly specialized and very arcane!!
            setPlayerDone(player, getGame().getEntitiesOwnedBy(player) <= 0);
        }
        transmitAllPlayerDones();
    }

    /**
     * Rolls initiative for all teams.
     */
    void rollInitiative() {
        TurnOrdered.rollInitiative(game.getTeams(), false);
        transmitAllPlayerUpdates();
    }

    private Packet createAllReportsPacket(Player recipient) {
        return new Packet(PacketCommand.SENDING_REPORTS_ALL, game.getGameReport().createFilteredReport(recipient));
    }

    public void clearPendingReports() {
        pendingReports.clear();
    }

    protected List<Report> getPendingReports() {
        return pendingReports;
    }

    void addPendingReportsToGame() {
        game.addReports(pendingReports);
    }

    /**
     * Tries to change to the next turn. If there are no more turns, ends the
     * current phase. If the player whose turn it is next is not connected, we
     * allow the other players to skip that player.
     */
    private void changeToNextTurn(int prevPlayerId) {
//        boolean minefieldPhase = game.getPhase().isDeployMinefields();
//        boolean artyPhase = game.getPhase().isSetArtilleryAutohitHexes();
//
//        GameTurn nextTurn = null;
//        Entity nextEntity = null;
//        while (game.hasMoreTurns() && (null == nextEntity)) {
//            nextTurn = game.changeToNextTurn();
//            nextEntity = game.getEntity(game.getFirstEntityNum(nextTurn));
//            if (minefieldPhase || artyPhase) {
//                break;
//            }
//        }
//
//        // if there aren't any more valid turns, end the phase
//        // note that some phases don't use entities
//        if (((null == nextEntity) && !minefieldPhase) || ((null == nextTurn) && minefieldPhase)) {
//            endCurrentPhase();
//            return;
//        }
//
//        Player player = game.getPlayer(nextTurn.getPlayerNum());
//
//        if ((player != null) && (game.getEntitiesOwnedBy(player) == 0)) {
//            endCurrentTurn(null);
//            return;
//        }
//
//        if (prevPlayerId != -1) {
//            send(packetHelper.createTurnIndexPacket(prevPlayerId));
//        } else {
//            send(packetHelper.createTurnIndexPacket(player != null ? player.getId() : Player.PLAYER_NONE));
//        }
//
//        if ((null != player) && player.isGhost()) {
//            sendGhostSkipMessage(player);
//        } else if ((null == game.getFirstEntity()) && (null != player) && !minefieldPhase && !artyPhase) {
//            sendTurnErrorSkipMessage(player);
//        }
    }

}
