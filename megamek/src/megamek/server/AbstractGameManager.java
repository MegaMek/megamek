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

import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import org.apache.logging.log4j.LogManager;

abstract class AbstractGameManager implements IGameManager {

    protected final GameManagerPacketHelper packetHelper = new GameManagerPacketHelper(this);
    protected final GameManagerSaveHelper saveHandler = new GameManagerSaveHelper(this);
    protected final AutosaveService autoSaveService = new AutosaveService(this);

    /**
     * Sends the given packet to all connections (all connected Clients = players).
     * @see Server#send(Packet)
     */
    protected final void send(Packet packet) {
        Server.getServerInstance().send(packet);
    }

    /**
     * Sends the given packet to the given connection (= player ID).
     * @see Server#send(int, Packet)
     */
    protected final void send(int connId, Packet p) {
        Server.getServerInstance().send(connId, p);
    }

    @Override
    public void handlePacket(int connId, Packet packet) {
        switch (packet.getCommand()) {
            case PLAYER_READY:
                receivePlayerDone(packet, connId);
                send(packetHelper.createPlayerDonePacket(connId));
                checkReady();
                break;
        }
    }

    /**
     * Ends this phase and moves on to the next.
     */
    protected abstract void endCurrentPhase();

    /**
     * Do anything we need to work through the current phase, such as give a turn to the
     * first player to play.
     */
    protected abstract void executeCurrentPhase();

    /**
     * Prepares for the game's current phase. This typically involves resetting the states of
     * units in the game and making sure the clients have the information they need for the new phase.
     */
    protected abstract void prepareForCurrentPhase();

    /**
     * Switches to the given new Phase and preforms preparation, checks if it should be skipped
     * and executes it.
     */
    protected final void changePhase(GamePhase newPhase) {
        getGame().setLastPhase(getGame().getPhase());
        getGame().setPhase(newPhase);

        prepareForCurrentPhase();

        if (getGame().shouldSkipCurrentPhase()) {
            endCurrentPhase();
        } else {
            // tell the players about the new phase
            send(packetHelper.createPhaseChangePacket());

            executeCurrentPhase();
        }
    }

    /**
     * Called when a player declares that they are "done". By default, this method advances to the next phase, if
     * <BR>- all non-ghost, non-observer players are done,
     * <BR>- the present phase does not use turns (e.g. if it's a report phase), and
     * <BR>- we are not in an empty lobby (= no units at all).
     * <BR>In other circumstances, ending the current phase is triggered elsewhere. Note that specifically,
     * ghost players are not checked for their status here so the game can advance through non-turn (report)
     * phases even with ghost players.
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

    /** @return True when the game is in the lobby phase and is empty (no units present). */
    protected boolean isEmptyLobby() {
        return getGame().getPhase().isLounge() && getGame().getInGameObjects().isEmpty();
    }

    /**
     * Sets a player's ready status as received from the Client. This method does not perform any
     * follow-up actions.
     */
    private void receivePlayerDone(Packet packet, int connIndex) {
        boolean ready = packet.getBooleanValue(0);
        Player player = getGame().getPlayer(connIndex);
        if (null != player) {
            player.setDone(ready);
        } else {
            LogManager.getLogger().error("Tried to set done status of non-existent player!");
        }
    }

    /**
     * Sends out the player object to all players. Private info of the given player
     * is redacted before being sent to other players.
     *
     * @param player The player whose information is to be shared
     * @see #transmitAllPlayerUpdates()
     */
    protected void transmitPlayerUpdate(Player player) {
        Server.getServerInstance().transmitPlayerUpdate(player);
    }

    /**
     * Shares all player objects with all players. Private info is redacted before being sent
     * to other players.
     *
     * @see #transmitPlayerUpdate(Player)
     */
    protected void transmitAllPlayerUpdates() {
        getGame().getPlayersList().forEach(this::transmitPlayerUpdate);
    }

    /**
     * Performs an automatic save (does not check the autosave settings - the autosave will simply be done).
     * Depending on the settings, the "autosave" filename is appended
     * with a timestamp and/or a chat message is sent announcing the autosave.
     */
    protected void autoSave() {
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
     * @see IGame#getTurnsList()
     */
    void sendCurrentTurns() {
        send(packetHelper.createTurnVectorPacket());
    }
}
