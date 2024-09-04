/**
 *
 */
package megamek.server.commands;

import megamek.common.util.AddBotUtil;
import megamek.server.totalwarfare.TWGameManager;
import megamek.server.Server;

/**
 * @author dirk
 */
public class AddBotCommand extends ServerCommand {

    private final TWGameManager gameManager;

    /**
     * @param server the megamek.server.Server.
     */
    public AddBotCommand(Server server, TWGameManager gameManager) {
        super(server, AddBotUtil.COMMAND, AddBotUtil.USAGE);
        this.gameManager = gameManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        String result = new AddBotUtil().addBot(args, gameManager.getGame(), server.getHost(), server.getPort());
        server.sendServerChat(connId, result);
    }
}
