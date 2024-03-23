package megamek.common.strategicBattleSystems;

import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.net.packets.Packet;
import megamek.server.IGameManager;
import megamek.server.Server;
import megamek.server.commands.ServerCommand;
import org.apache.logging.log4j.LogManager;

import java.util.List;

public class SBFGameManager implements IGameManager {

    private IGame game;

    @Override
    public IGame getGame() {
        return game;
    }

    @Override
    public void setGame(IGame g) {
        if (!(g instanceof SBFGame)) {
            LogManager.getLogger().error("Attempted to set game to incorrect class.");
            return;
        }
        game = g;
    }

    @Override
    public void resetGame() {

    }

    @Override
    public void disconnect(Player player) {

    }

    @Override
    public void sendCurrentInfo(int connId) {

    }

    @Override
    public void saveGame(String fileName) {

    }

    @Override
    public void sendSaveGame(int connId, String fileName, String localPath) {

    }

    @Override
    public void removeAllEntitiesOwnedBy(Player player) {

    }

    @Override
    public void handlePacket(int connId, Packet packet) {

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
        return null;
    }

    @Override
    public void addReport(Report r) {

    }

    @Override
    public void calculatePlayerInitialCounts() {

    }
}
