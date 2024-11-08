package megamek.server.commands;

import megamek.server.totalwarfare.TWGameManager;

public interface IsGM {

    TWGameManager getGameManager();

    default boolean isGM(int connId) {
        return getGameManager().getGame().getPlayer(connId).getGameMaster();
    }

}
