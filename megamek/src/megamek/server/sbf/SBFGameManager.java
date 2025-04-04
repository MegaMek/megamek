/*
 * Copyright (c) 2024-2025 - The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import megamek.common.IGame;
import megamek.common.InGameObject;
import megamek.common.Player;
import megamek.common.ReportEntry;
import megamek.common.TurnOrdered;
import megamek.common.actions.EntityAction;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.options.SBFRuleOptions;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.common.strategicBattleSystems.SBFReportEntry;
import megamek.common.strategicBattleSystems.SBFRuleOptionsUser;
import megamek.common.strategicBattleSystems.SBFTurn;
import megamek.logging.MMLogger;
import megamek.server.AbstractGameManager;
import megamek.server.Server;
import megamek.server.commands.ServerCommand;

/**
 * This class manages an SBF game on the server side. As of 2024, this is under construction.
 */
public final class SBFGameManager extends AbstractGameManager implements SBFRuleOptionsUser {
    private static final MMLogger logger = MMLogger.create(SBFGameManager.class);

    private SBFGame game;

    private final List<SBFReportEntry> pendingReports = new ArrayList<>();

    record PendingPacket(int recipient, Packet packet) {
    }

    private final List<PendingPacket> pendingPackets = new ArrayList<>();

    final SBFPhaseEndManager phaseEndManager = new SBFPhaseEndManager(this);
    final SBFPhasePreparationManager phasePreparationManager = new SBFPhasePreparationManager(this);
    final SBFMovementProcessor movementProcessor = new SBFMovementProcessor(this);
    final SBFAttackProcessor attackProcessor = new SBFAttackProcessor(this);
    final SBFActionsProcessor actionsProcessor = new SBFActionsProcessor(this);
    final SBFInitiativeHelper initiativeHelper = new SBFInitiativeHelper(this);
    final SBFUnitUpdateHelper unitUpdateHelper = new SBFUnitUpdateHelper(this);
    final SBFDetectionHelper detectionHelper = new SBFDetectionHelper(this);

    @Override
    public void handlePacket(int connId, Packet packet) {
        super.handlePacket(connId, packet);

        switch (packet.getCommand()) {
            case ENTITY_MOVE:
                receiveMovement(packet, connId);
                break;
            case ENTITY_ATTACK:
                receiveAttack(packet, connId);
                break;
            default:
                break;
        }

        logger.info("Leaving handle packet: {}", packet.getCommand());
        logger.info(pendingPackets);
        sendPendingPackets();
    }

    /**
     * Sends all pending packets to eligible players and clears out the pending packets.
     */
    private void sendPendingPackets() {
        // packets must be sorted/filtered according to recipient for double blind games
        // each player must receive the packets directed at them as well as any
        // undirected packets
        // in the order they were stored in pendingPackets
        if (!pendingPackets.isEmpty()) {
            reducePendingPackets();
            // Send each player what they should receive ...
            for (int playerId : game.getPlayersList().stream().map(Player::getId).toList()) {
                List<Packet> packets = pendingPackets.stream()
                                             // ... including packets to PLAYER_NONE; these go to every player
                                             .filter(p -> (p.recipient == Player.PLAYER_NONE) ||
                                                                (p.recipient == playerId))
                                             .map(PendingPacket::packet)
                                             .toList();
                // the redundant new ArrayList is necessary to prevent an xstream error
                super.send(playerId, new Packet(PacketCommand.MULTI_PACKET, new ArrayList<>(packets)));
            }
            pendingPackets.clear();
        }
    }

    private void reducePendingPackets() {
        // TODO remove redundant packets, maybe consolidate packets
    }

    void addPendingPacket(int recipient, Packet packet) {
        pendingPackets.add(new PendingPacket(recipient, packet));
    }

    void addPendingPacket(Packet packet) {
        pendingPackets.add(new PendingPacket(Player.PLAYER_NONE, packet));
    }

    public SBFGame getGame() {
        return game;
    }

    @Override
    public void setGame(IGame g) {
        if (!(g instanceof SBFGame)) {
            logger.fatal("Attempted to set game to incorrect class.");
            return;
        }
        game = (SBFGame) g;
    }

    @Override
    public void resetGame() {
    }

    @Override
    public void disconnect(Player player) {
    }

    @Override
    public void removeAllEntitiesOwnedBy(Player player) {
    }

    @Override
    public void handleCfrPacket(Server.ReceivedPacket rp) {
    }

    @Override
    public void requestGameMaster(Player player) {
    }

    @Override
    public void requestTeamChange(int teamId, Player player) {
    }

    @Override
    public List<ServerCommand> getCommandList(Server server) {
        return Collections.emptyList();
    }

    @Override
    public void addReport(ReportEntry r) {
        pendingReports.add((SBFReportEntry) r);
    }

    @Override
    public void calculatePlayerInitialCounts() {
    }

    @Override
    public void requestTeamChangeForPlayer(int teamID, Player player) {

    }

    /**
     * Creates a packet containing all entities, including wrecks, visible to the player in a blind game
     */
    Packet createGameStartUnitPacket(Player recipient) {
        return new Packet(PacketCommand.SENDING_ENTITIES,
              new ArrayList<>(getVisibleUnits(recipient)),
              getGame().getGraveyard(),
              // TODO: must add Sensor blips of all kinds as a separate list of stuff
              getGame().getForces());
    }

    @Override
    public void sendCurrentInfo(int connId) {
        send(connId, packetHelper.createGameSettingsPacket());

        Player player = getGame().getPlayer(connId);
        if (null != player) {
            send(connId, new Packet(PacketCommand.SENDING_MINEFIELDS, player.getMinefields()));

            if (getGame().getPhase().isLounge()) {
                // send(connId, createMapSettingsPacket());
                // send(createMapSizesPacket());

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
                // setPlayerDone(player, getGame().getEntitiesOwnedBy(player) <= 0);
                send(connId, packetHelper.createPhaseChangePacket());
            }

            send(connId, packetHelper.createPlanetaryConditionsPacket());
            //
            if (game.getPhase().isFiring() ||
                      game.getPhase().isTargeting() ||
                      game.getPhase().isOffboard() ||
                      game.getPhase().isPhysical()) {
                // can't go above, need board to have been sent
                // send(connId, packetHelper.createAttackPacket(getGame().getActionsVector(),
                // false));
                // send(connId, packetHelper.createAttackPacket(getGame().getChargesVector(),
                // true));
                // send(connId, packetHelper.createAttackPacket(getGame().getRamsVector(),
                // true));
                // send(connId,
                // packetHelper.createAttackPacket(getGame().getTeleMissileAttacksVector(),
                // true));
            }
            //
            if (getGame().getPhase().usesTurns() && getGame().hasMoreTurns()) {
                send(packetHelper.createTurnListPacket());
                addPendingPacket(packetHelper.createTurnIndexPacket(connId));
            } else if (!getGame().getPhase().isLounge() && !getGame().getPhase().isStartingScenario()) {
                endCurrentPhase();
            }
            //
            // send(connId, createArtilleryPacket(player));
            // send(connId, createFlarePacket());
            // send(connId, createSpecialHexDisplayPacket(connId));
            // send(connId, new Packet(PacketCommand.PRINCESS_SETTINGS,
            // getGame().getBotSettings()));

            // This method is not called through normal packet handling, so it must send
            // packets actively
            sendPendingPackets();
        }
    }

    @Override
    protected void endCurrentPhase() {
        logger.info("Ending phase {}", game.getPhase());
        phaseEndManager.managePhase();
    }

    @Override
    protected void prepareForCurrentPhase() {
        logger.info("Preparing phase {}", game.getPhase());
        phasePreparationManager.managePhase();
    }

    @Override
    protected void executeCurrentPhase() {
        logger.info("Executing phase {}", game.getPhase());
        switch (game.getPhase()) {
            case EXCHANGE:
                resetPlayersDone();
                // // Update initial BVs, as things may have been modified in lounge
                // for (Entity e : game.getEntitiesVector()) {
                // e.setInitialBV(e.calculateBattleValue(false, false));
                // }
                calculatePlayerInitialCounts();
                game.setupTeams();
                // applyBoardSettings();
                game.getPlanetaryConditions().determineWind();
                send(packetHelper.createPlanetaryConditionsPacket());
                send(packetHelper.createBoardsPacket());
                game.setupDeployment();
                // game.setVictoryContext(new HashMap<>());
                // game.createVictoryConditions();
                // // some entities may need to be checked and updated
                // checkEntityExchange();
                break;
            case MOVEMENT:
                // write Movement Phase header to report
                addReport(new SBFReportEntry(2000)); // , Report.PUBLIC));
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
                changeToNextTurn(-1); // TODO what is the prev player good for??
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
        // FIXME This is highly unclear why not in report but in victory
        if ((getGame().getPhase().isReport()) && (!getGame().getPhase().isVictory())) {
            return;
        }

        for (Player player : game.getPlayersList()) {
            setPlayerDone(player, false);
        }

        transmitAllPlayerDones();
    }

    private void setPlayerDone(Player player, boolean normalDone) {
        // FIXME This is highly specialized and very arcane!!
        if (getGame().getPhase().isReport() &&
                  getGame().getOptions().booleanOption(OptionsConstants.BASE_GM_CONTROLS_DONE_REPORT_PHASE) &&
                  getGame().getPlayersList().stream().filter(p -> p.isGameMaster()).count() > 0) {
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
     * Called at the beginning of certain phases to make every active player not ready.
     */
    void resetActivePlayersDone() {
        for (Player player : game.getPlayersList()) {
            // FIXME This is highly specialized and very arcane!!
            setPlayerDone(player, getGame().getEntitiesOwnedBy(player) <= 0);
        }
        transmitAllPlayerDones();
    }

    /**
     * Rolls initiative for all teams.
     */
    void rollInitiative() {
        // I couldn't find confirmation whether Combat Sense worked at a SBF-scale, and not was wanting to change Juliez
        // WIP too much, I opted to just pass in an empty map here. -- Illiani (April 3rd 2025)
        TurnOrdered.rollInitiative(game.getTeams(), false, new HashMap<>());
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

    List<SBFReportEntry> getPendingReports() {
        return pendingReports;
    }

    void addPendingReportsToGame() {
        game.addReports(pendingReports);
    }

    /**
     * Tries to change to the next turn. If there are no more turns, ends the current phase. If the player whose turn it
     * is next is not connected, we allow the other players to skip that player.
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
                // send(packetHelper.createTurnIndexPacket(prevPlayerId));
                addPendingPacket(packetHelper.createTurnIndexPacket(prevPlayerId));
            } else {
                addPendingPacket(packetHelper.createTurnIndexPacket(player.map(Player::getId)
                                                                          .orElse(Player.PLAYER_NONE)));
                // send(packetHelper.createTurnIndexPacket(player.map(Player::getId).orElse(Player.PLAYER_NONE)));
            }

            if (player.isPresent() && player.get().isGhost()) {
                sendGhostSkipMessage(player.get());
                // } else if ((null == game.getFirstEntity()) && (null != player) &&
                // !minefieldPhase && !artyPhase) {
                // sendTurnErrorSkipMessage(player);
            }
        }
    }

    private List<InGameObject> getVisibleUnits(Player viewer) {
        return game.getFullyVisibleUnits(viewer);
    }

    /**
     * Send the round report to all connected clients.
     */
    public void sendReport() {
        // EmailService mailer = Server.getServerInstance().getEmailService();
        // if (mailer != null) {
        // for (var player: mailer.getEmailablePlayers(game)) {
        // try {
        // var reports = filterReportVector(vPhaseReport, player);
        // var message = mailer.newReportMessage(game, reports, player);
        // mailer.send(message);
        // } catch (Exception ex) {
        // logger.error("Error sending round report", ex);
        // }
        // }
        // }
        game.getPlayersList().forEach(player -> send(player.getId(), createReportPacket(player)));
    }

    @Override
    public SBFRuleOptions getOptions() {
        return game.getOptions();
    }

    /**
     * Receives an entity movement packet, and if valid, executes it and ends the current turn.
     */
    private void receiveMovement(Packet packet, int connId) {
        var movePath = (SBFMovePath) packet.getObject(0);
        movePath.restore(game);
        Optional<SBFFormation> formationInfo = game.getFormation(movePath.getEntityId());
        if (formationInfo.isEmpty()) {
            logger.error("Malformed packet {}", packet);
            return;
        }
        SBFTurn turn = game.getTurn();
        if ((turn == null) || !turn.isValid(connId, formationInfo.get(), game)) {
            logger.error("It is not player {}'s turn! ", connId);
            return;
        }

        movementProcessor.processMovement(movePath, formationInfo.get());
    }

    /**
     * Called when the current player has done his current turn and the turn counter needs to be advanced.
     */
    void endCurrentTurn(SBFFormation entityUsed) {
        final int playerId = (null == entityUsed) ? Player.PLAYER_NONE : entityUsed.getOwnerId();
        changeToNextTurn(playerId);
    }

    @Override
    protected void transmitAllPlayerDones() {
        getGame().getPlayersList()
              .forEach(player -> addPendingPacket(player.getId(), packetHelper.createPlayerDonePacket(player.getId())));
        // getGame().getPlayersList().forEach(player ->
        // send(packetHelper.createPlayerDonePacket(player.getId())));
    }

    public void send(Packet packet) {
        addPendingPacket(packet);
    }

    /**
     * Sends the given packet to the given connection (= player ID).
     *
     * @see Server#send(int, Packet)
     */
    public void send(int connId, Packet p) {
        addPendingPacket(connId, p);
    }

    /**
     * Sends out the player object to all players. Private info of the given player is redacted before being sent to
     * other players.
     *
     * @param player The player whose information is to be shared
     *
     * @see #transmitAllPlayerUpdates() //TODO: wonder if pending packets can be extended to TW //TODO: might work
     *       easily by overriding send; must send CFR packets immediately
     */
    protected void transmitPlayerUpdate(Player player) {
        int playerId = player.getId();
        for (Player player1 : game.getPlayersList()) {

            var destPlayer = player;

            if (playerId != player1.getId()) {
                // Sending the player's data to another player's
                // connection, need to redact any private data
                destPlayer = player.copy();
                destPlayer.redactPrivateData();
            }
            send(player1.getId(), new Packet(PacketCommand.PLAYER_UPDATE, playerId, destPlayer));
        }
    }

    /**
     * Info at {@link SBFUnitUpdateHelper#sendUnitUpdate(InGameObject)}
     */
    void sendUnitUpdate(InGameObject unit) {
        unitUpdateHelper.sendUnitUpdate(unit);
    }

    /**
     * Updates all units to all players, taking into account double blind filtering.
     */
    void entityAllUpdate() {
        for (Player player : game.getPlayersList()) {
            send(player.getId(), createGameStartUnitPacket(player));
        }
    }

    private void repeatTurn(int connId) {
        send(connId, packetHelper.createTurnListPacket());
        SBFTurn turn = game.getTurn();
        send(connId, packetHelper.createTurnIndexPacket((turn == null) ? Player.PLAYER_NONE : turn.playerId()));
    }

    @SuppressWarnings("unchecked")
    void receiveAttack(Packet packet, int connId) {
        var attacks = (List<EntityAction>) packet.getObject(1);
        int formationId = (int) packet.getObject(0);
        Optional<SBFFormation> formationInfo = game.getFormation(formationId);

        if (formationInfo.isEmpty() ||
                  !attacks.stream().map(EntityAction::getEntityId).allMatch(id -> id == formationId)) {
            logger.error("Invalid formation ID or diverging attacker IDs");
            repeatTurn(connId); // TODO: This is untested; questionable if this can save a game after an error
            return;
        }

        for (EntityAction action : attacks) {
            if (!validateEntityAction(action, connId)) {
                repeatTurn(connId);
                return;
            }
        }

        // is this the right phase?
        if (!getGame().getPhase().isFiring() &&
                  !getGame().getPhase().isPhysical() &&
                  !getGame().getPhase().isTargeting() &&
                  !getGame().getPhase().isOffboard()) {
            logger.error("Server got attack packet in wrong phase");
            return;
        }

        // looks like mostly everything's okay
        attackProcessor.processAttacks(attacks, formationInfo.get());
    }

    private boolean validateEntityAction(EntityAction action, int connId) {
        // TODO unify firing/movement validity
        Optional<SBFFormation> formationInfo = game.getFormation(action.getEntityId());
        if (formationInfo.isEmpty()) {
            logger.error("Incorrect formation ID {}", action.getEntityId());
            return false;
        }
        SBFTurn turn = game.getTurn();
        if ((turn == null) || !turn.isValid(connId, formationInfo.get(), game)) {
            logger.error("It is not player {}'s turn! ", connId);
            return false;
        }

        return true;
    }

    /**
     * Sends the game's pending actions to all Clients for them to replace any previous actions
     */
    void sendPendingActions() {
        send(new Packet(PacketCommand.ACTIONS, new ArrayList<>(game.getActionsVector())));
    }
}
