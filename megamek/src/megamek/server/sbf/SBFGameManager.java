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
package megamek.server.sbf;

import megamek.common.*;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.options.SBFRuleOptions;
import megamek.common.strategicBattleSystems.*;
import megamek.server.AbstractGameManager;
import megamek.server.Server;
import megamek.server.commands.ServerCommand;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class manages an SBF game on the server side. As of 2024, this is under construction.
 */
public final class SBFGameManager extends AbstractGameManager implements SBFRuleOptionsUser {

    private SBFGame game;

    private final List<Report> pendingReports = new ArrayList<>();

    record PendingPacket(int recipient, Packet packet) { }
    private final List<PendingPacket> pendingPackets = new ArrayList<>();

    private final SBFPhaseEndManager phaseEndManager = new SBFPhaseEndManager(this);
    private final SBFPhasePreparationManager phasePreparationManager = new SBFPhasePreparationManager(this);
    private final SBFMovementProcessor movementProcessor = new SBFMovementProcessor(this);
    final SBFInitiativeHelper initiativeHelper = new SBFInitiativeHelper(this);

    @Override
    public void handlePacket(int connId, Packet packet) {
        super.handlePacket(connId, packet);

        switch (packet.getCommand()) {
            case ENTITY_MOVE:
                receiveMovement(packet, connId);
                break;
        }

        sendPendingPackets();
        LogManager.getLogger().info("Leaving handle packet: {}", packet.getCommand());
    }

    /**
     * Sends all pending packets to eligible players and clears out the pending packets.
     */
    private void sendPendingPackets() {
        // packets must be sorted/filtered according to recipient for double blind games
        // each player must receive the packets directed at them as well as any undirected packets
        // in the order they were stored in pendingPackets
        if (!pendingPackets.isEmpty()) {
            // collect all recipients; this includes Player.PLAYER_NONE for packets to everyone
            Set<Integer> playerIds = pendingPackets.stream().map(PendingPacket::recipient).collect(Collectors.toSet());
            // Now send each player what they should receive ...
            for (int player : playerIds) {
                // ... but not to PLAYER_NONE (= to everyone); send only to each player individually
                if (player != Player.PLAYER_NONE) {
                    List<Packet> packets = pendingPackets.stream()
                            // include packets to PLAYER_NONE; these may go to every player
                            .filter(p -> (p.recipient == Player.PLAYER_NONE) || (p.recipient == player))
                            .map(PendingPacket::packet)
                            .toList();
                    // the seemingly redundant new ArrayList is necessary to prevent an xstream error
                    send(player, new Packet(PacketCommand.MULTI_PACKET, new ArrayList<>(packets)));
                }
            }
            pendingPackets.clear();
        }
    }

    void addPendingPacket(int recipient, Packet packet) {
        pendingPackets.add(new PendingPacket(recipient, packet));
    }

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

    /**
     * Creates a packet containing all entities, including wrecks, visible to
     * the player in a blind game
     */
    private Packet createGameStartUnitPacket(Player recipient) {
        return new Packet(PacketCommand.SENDING_ENTITIES,
                getVisibleUnits(recipient),
                getGame().getGraveyard(),
                //TODO: must add Sensor blips of all kinds as a separate list of stuff
                getGame().getForces());
    }

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
                send(connId, createGameStartUnitPacket(player));
            } else {
                send(connId, packetHelper.createCurrentRoundNumberPacket());
                send(connId, packetHelper.createBoardsPacket());
                send(connId, createAllReportsPacket(player));

                // Send entities *before* other phase changes.
                send(connId, createGameStartUnitPacket(player));
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
        LogManager.getLogger().info("Ending phase {}", game.getPhase());
        phaseEndManager.managePhase();
    }

    @Override
    protected void prepareForCurrentPhase() {
        LogManager.getLogger().info("Preparing phase {}", game.getPhase());
        phasePreparationManager.managePhase();
    }

    @Override
    protected void executeCurrentPhase() {
        LogManager.getLogger().info("Executing phase {}", game.getPhase());
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

    private Packet createReportPacket(Player recipient) {
        return new Packet(PacketCommand.SENDING_REPORTS, pendingReports);
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
        if (!game.hasMoreTurns()) {
            endCurrentPhase();
            return;
        }

        SBFTurn nextTurn = game.changeToNextTurn();
        boolean isValidTurn = nextTurn.isValid(game);
        while (game.hasMoreTurns() && !isValidTurn) {
            nextTurn = game.changeToNextTurn();
            isValidTurn = nextTurn.isValid(game);
        }

        if (!isValidTurn) {
            endCurrentPhase();
        } else {
            Optional<Player> player = game.getPlayerFor(nextTurn);
            if (prevPlayerId != Player.PLAYER_NONE) {
                send(packetHelper.createTurnIndexPacket(prevPlayerId));
            } else {
                send(packetHelper.createTurnIndexPacket(player.map(Player::getId).orElse(Player.PLAYER_NONE)));
            }

            if (player.isPresent() && player.get().isGhost()) {
                sendGhostSkipMessage(player.get());
//            } else if ((null == game.getFirstEntity()) && (null != player) && !minefieldPhase && !artyPhase) {
//                sendTurnErrorSkipMessage(player);
            }
        }
    }

    private List<InGameObject> getVisibleUnits(Player viewer) {
        if (usesDoubleBlind()) {
            return game.getInGameObjects().stream()
                    .filter(unit -> game.isVisible(viewer.getId(), unit.getId()))
                    .collect(Collectors.toList());
        } else {
            return game.getInGameObjects();
        }
    }

    /**
     * Send the round report to all connected clients.
     */
    public void sendReport() {
//        EmailService mailer = Server.getServerInstance().getEmailService();
//        if (mailer != null) {
//            for (var player: mailer.getEmailablePlayers(game)) {
//                try {
//                    var reports = filterReportVector(vPhaseReport, player);
//                    var message = mailer.newReportMessage(game, reports, player);
//                    mailer.send(message);
//                } catch (Exception ex) {
//                    LogManager.getLogger().error("Error sending round report", ex);
//                }
//            }
//        }
        game.getPlayersList().forEach(player -> send(player.getId(), createReportPacket(player)));
    }

    @Override
    public SBFRuleOptions getOptions() {
        return game.getOptions();
    }

    /**
     * Receives an entity movement packet, and if valid, executes it and ends
     * the current turn.
     */
    private void receiveMovement(Packet packet, int connId) {
        var movePath = (SBFMovePath) packet.getObject(0);
        movePath.restore(game);
        Optional<SBFFormation> formationInfo = game.getFormation(movePath.getEntityId());
        if (formationInfo.isEmpty()) {
            LogManager.getLogger().error("Malformed packet {}", packet);
            return;
        }
        SBFTurn turn = game.getTurn();
        if ((turn == null) || !turn.isValid(connId, formationInfo.get(), game)) {
            LogManager.getLogger().error("It is not player {}'s turn! ", connId);
            return;
        }

        movementProcessor.processMovement(movePath, formationInfo.get());
    }

    /**
     * Called when the current player has done his current turn and the turn
     * counter needs to be advanced.
     */
    void endCurrentTurn(SBFFormation entityUsed) {
        final int playerId = null == entityUsed ? Player.PLAYER_NONE : entityUsed.getOwnerId();
        changeToNextTurn(playerId);
    }

    Packet createFormationPacket(SBFFormation formation) {
        return new Packet(PacketCommand.ENTITY_UPDATE, formation);
    }

    Packet createUnitPacket(int unitId) {
        if (game.getInGameObject(unitId).isEmpty()) {
            LogManager.getLogger().error("No unit found for id {}! ", unitId);
        }
        return new Packet(PacketCommand.ENTITY_UPDATE, unitId, game.getInGameObject(unitId).get());
    }
}
