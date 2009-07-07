/**
 * 
 */
package megamek.server.commands;

import java.util.Enumeration;

import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.ui.AWT.BotGUI;
import megamek.common.Player;
import megamek.server.Server;

/**
 * @author dirk
 */
public class AddBotCommand extends ServerCommand {

    /**
     * @param server the megamek.server.Server.
     */
    public AddBotCommand(Server server) {
        super(
                server,
                "replacePlayer",
                "Replaces a player who is a ghost with a bot. Usage /replacePlayer name, to replace the player named name. they must be a ghost.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        if (args.length < 2) {
            server.sendServerChat(connId, "You must specify a player name.");
            return;
        }
        String playerName = args[1];
        for(int i = 2; i < args.length; i++) {
            playerName = playerName + " " + args[i];
        }
        
        Player target = null;
        for (Enumeration<Player> i = server.getGame().getPlayers(); i
                .hasMoreElements();) {
            Player player = i.nextElement();
            if (player.getName().equals(playerName)) {
                target = player;
            }
        }

        if (target == null) {
            server.sendServerChat(connId, "No player with the name '" + args[1]
                    + "'.");
            return;
        }

        if (target.isGhost()) {
            BotClient c = new TestBot(target.getName(), server.getHost(),
                    server.getPort());
            c.game.addGameListener(new BotGUI(c));
            try {
                c.connect();
            } catch (Exception e) {
                server.sendServerChat(connId, "Bot failed to connect.");
            }
            c.retrieveServerInfo();
            server.sendServerChat("Bot has replaced " + target.getName() + ".");
            return;
        }

        server.sendServerChat(connId, "Player " + target.getName()
                + " is not a ghost.");
    }

}
