/**
 *
 */
package megamek.server.commands;

import megamek.common.util.AddBotUtil;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * @author dirk
 */
public class AddBotCommand extends ServerCommand {

    private final GameManager gameManager;

    /**
     * @param server the megamek.server.Server.
     */
    public AddBotCommand(Server server, GameManager gameManager) {
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
