package megamek.server;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.net.packets.Packet;
import org.apache.logging.log4j.LogManager;

abstract class AbstractGameManager implements IGameManager {

    protected final GameManagerPacketHelper packetHelper = new GameManagerPacketHelper(this);

    protected final void send(Packet p) {
        Server.getServerInstance().send(p);
    }

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

    protected abstract void endCurrentPhase();

    protected abstract void changePhase(GamePhase newPhase);

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

        if (!getGame().getPhase().hasTurns() && !isEmptyLobby()) {
            endCurrentPhase();
        }
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
}
