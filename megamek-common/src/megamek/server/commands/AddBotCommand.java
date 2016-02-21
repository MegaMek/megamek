/**
 *
 */
package megamek.server.commands;

import megamek.common.util.IAddBotUtil;
import megamek.server.Server;

/**
 * @author dirk
 */
public class AddBotCommand extends ServerCommand {
	
    public static String COMMAND = "replacePlayer";
    public static String USAGE = "Not Implemented. Yell at Kurios please to fix this.";

    /**
     * @param server the megamek.server.Server.
     */
    public AddBotCommand(Server server) {
        super(server, AddBotCommand.COMMAND, AddBotCommand.USAGE);
    }
    
    private static IAddBotUtil botManager; 
    
    public static void setBotManager(IAddBotUtil botManager){
    	AddBotCommand.botManager = botManager;
    	COMMAND = botManager.getCommand();
    	USAGE = botManager.getUsage();
    }
    /*
     * (non-Javadoc)
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
    	if(botManager != null){
        String result = botManager.addBot(args, server.getGame(), server.getHost(), server.getPort());
        server.sendServerChat(connId, result);
    	}
    }
}
