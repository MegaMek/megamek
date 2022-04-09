package megamek.server.victory;

import megamek.server.Server;

public class VictoryChecker {
    /**
     * Returns true if victory conditions have been met. Victory conditions are
     * when there is only one player left with mechs or only one team. will also
     * add some reports to reporting
     * @param server
     */
    public static boolean victory(Server server) {
        VictoryResult vr = server.getGame().getVictoryResult();
        vr.processVictory(server.getGame()).forEach(server::addReport);
        return vr.victory();
    }
}
