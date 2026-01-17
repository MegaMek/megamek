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

package megamek.server;

import megamek.common.game.IGame;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;
import megamek.server.trigger.TriggerSituation;

public abstract class AbstractGameManager implements IGameManager {
    private static final MMLogger logger = MMLogger.create(AbstractGameManager.class);

    protected final GameManagerPacketHelper packetHelper = new GameManagerPacketHelper(this);
    protected final GameManagerSaveHelper saveHandler = new GameManagerSaveHelper(this);
    protected final AutosaveService autoSaveService = new AutosaveService(this);
    protected final GameManagerScriptedEventHelper scriptedEventHelper = new GameManagerScriptedEventHelper(this);

    /**
     * Sends the given packet to all connections (all connected Clients = players).
     *
     * @see Server#send(Packet)
     */
    @Override
    public void send(Packet packet) {
        Server.getServerInstance().send(packet);
    }

    /**
     * Sends the given packet to the given connection (= player ID).
     *
     * @see Server#send(int, Packet)
     */
    @Override
    public void send(int connId, Packet p) {
        Server.getServerInstance().send(connId, p);
    }

    @Override
    public void handlePacket(int connId, Packet packet) {
        if (packet.command() == PacketCommand.PLAYER_READY) {
            try {
                receivePlayerDone(packet, connId);
                send(packetHelper.createPlayerDonePacket(connId));
                checkReady();
            } catch (InvalidPacketDataException e) {
                logger.error("Invalid packet data:", e);
            }
        }
    }

    /**
     * Ends this phase and moves on to the next.
     */
    protected abstract void endCurrentPhase();

    /**
     * Do anything we need to work through the current phase, such as give a turn to the first player to play.
     */
    protected abstract void executeCurrentPhase();

    /**
     * Prepares for the game's current phase. This typically involves resetting the states of units in the game and
     * making sure the clients have the information they need for the new phase.
     */
    protected abstract void prepareForCurrentPhase();

    /**
     * Switches to the given new Phase and preforms preparation, checks if it should be skipped and executes it.
     */
    public final void changePhase(GamePhase newPhase) {
        if (getGame().getPhase().isExchange() || getGame().getPhase().isStartingScenario()) {
            scriptedEventHelper.processScriptedEvents(TriggerSituation.GAME_START);
        }
        if (newPhase.isInitiative()) {
            scriptedEventHelper.processScriptedEvents(TriggerSituation.ROUND_START);
        }
        if (getGame().getPhase().isEnd()) {
            // the endreport phase cannot be used here as it may be skipped
            scriptedEventHelper.processScriptedEvents(TriggerSituation.ROUND_END);
        }
        scriptedEventHelper.processScriptedEvents(TriggerSituation.PHASE_END);

        getGame().setLastPhase(getGame().getPhase());
        getGame().setPhase(newPhase);

        scriptedEventHelper.processScriptedEvents(TriggerSituation.PHASE_START);
        if (getGame().getPhase().isVictory()) {
            scriptedEventHelper.processScriptedEvents(TriggerSituation.GAME_END);
        }
        prepareForCurrentPhase();

        if (getGame().shouldSkipCurrentPhase()) {
            endCurrentPhase();
        } else {
            // tell the players about the new phase
            sendPhaseChange();
            executeCurrentPhase();
        }
    }

    protected void sendPhaseChange() {
        send(packetHelper.createPhaseChangePacket());
    }

    /**
     * Called when a player declares that they are "done". By default, this method advances to the next phase, if
     * <BR>
     * - all non-ghost, non-observer players are done,
     * <BR>
     * - the present phase does not use turns (e.g. if it's a report phase), and
     * <BR>
     * - we are not in an empty lobby (= no units at all).
     * <BR>
     * In other circumstances, ending the current phase is triggered elsewhere. Note that specifically, ghost players
     * are not checked for their status here so the game can advance through non-turn (report) phases even with ghost
     * players.
     */
    protected void checkReady() {
        for (Player player : getGame().getPlayersList()) {
            if (!player.isGhost() && !player.isObserver() && !player.isDone()) {
                return;
            }
        }

        if (!getGame().getPhase().usesTurns() && !isEmptyLobby()) {
            endCurrentPhase();
        }
    }

    /**
     * Sends out the player ready stats for all players to all connections
     */
    protected void transmitAllPlayerDones() {
        getGame().getPlayersList().forEach(player -> send(packetHelper.createPlayerDonePacket(player.getId())));
    }

    /**
     * @return True when the game is in the lobby phase and is empty (no units present).
     */
    protected boolean isEmptyLobby() {
        return getGame().getPhase().isLounge() && getGame().getInGameObjects().isEmpty();
    }

    /**
     * Sets a player's ready status as received from the Client. This method does not perform any follow-up actions.
     */
    private void receivePlayerDone(Packet packet, int connIndex) throws InvalidPacketDataException {
        boolean ready = packet.getBooleanValue(0);
        Player player = getGame().getPlayer(connIndex);
        if (null != player) {
            player.setDone(ready);
        } else {
            logger.error("Tried to set done status of non-existent player!");
        }
    }

    /**
     * Sends out the player object to all players. Private info of the given player is redacted before being sent to
     * other players.
     *
     * @param player The player whose information is to be shared
     *
     * @see #transmitAllPlayerUpdates()
     */
    protected void transmitPlayerUpdate(Player player) {
        Server.getServerInstance().transmitPlayerUpdate(player);
    }

    /**
     * Shares all player objects with all players. Private info is redacted before being sent to other players.
     *
     * @see #transmitPlayerUpdate(Player)
     */
    public void transmitAllPlayerUpdates() {
        getGame().getPlayersList().forEach(this::transmitPlayerUpdate);
    }

    /**
     * Performs an automatic save (does not check the autosave settings - the autosave will simply be done). Depending
     * on the settings, the "autosave" filename is appended with a timestamp and/or a chat message is sent announcing
     * the autosave.
     */
    public void autoSave() {
        String fileName = "autosave";

        if (PreferenceManager.getClientPreferences().stampFilenames()) {
            fileName = StringUtil.addDateTimeStamp(fileName);
        }

        saveGame(fileName, getGame().getOptions().booleanOption(OptionsConstants.BASE_AUTOSAVE_MSG));
    }

    @Override
    public void saveGame(String fileName, boolean sendChat) {
        saveHandler.saveGame(fileName, sendChat);
    }

    @Override
    public void sendSaveGame(int connId, String sFile, String sLocalPath) {
        saveHandler.sendSaveGame(connId, sFile, sLocalPath);
    }

    public void sendChat(String origin, String message) {
        Server.getServerInstance().sendChat(origin, message);
    }

    public void sendChat(int connId, String origin, String message) {
        Server.getServerInstance().sendChat(connId, origin, message);
    }

    public void sendServerChat(String message) {
        Server.getServerInstance().sendServerChat(message);
    }

    public void sendServerChat(int connId, String message) {
        Server.getServerInstance().sendServerChat(connId, message);
    }

    /**
     * Sends the current list of player turns as stored in the game's turn list to the Clients.
     *
     * @see IGame#getTurnsList()
     */
    public void sendCurrentTurns() {
        send(packetHelper.createTurnListPacket());
    }

    /**
     * Increment's the server's game round and send it to all the clients
     */
    public void incrementAndSendGameRound() {
        getGame().incrementCurrentRound();
        send(packetHelper.createCurrentRoundNumberPacket());
    }

    public GameManagerPacketHelper getPacketHelper() {
        return packetHelper;
    }

    public AutosaveService getAutoSaveService() {
        return autoSaveService;
    }

    /**
     * Sends out a notification message indicating that a ghost player's turn may be skipped with the /skip command.
     *
     * @param ghost the Player who is ghosted. This value must not be null.
     */
    protected void sendGhostSkipMessage(Player ghost) {
        String message = String.format(
              "Player '%s' is disconnected. You may skip their current turn with the /skip command.",
              ghost.getName());
        sendServerChat(message);
    }
}
