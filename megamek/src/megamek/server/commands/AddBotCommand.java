/**
 *
 */
package megamek.server.commands;

import java.util.Enumeration;

import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.common.Player;
import megamek.common.logging.LogLevel;
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
                "Replaces a player who is a ghost with a bot. Usage /replacePlayer <-b:TestBot/Princess> name, to replace the player named name. They must be a ghost.  If the -b argument is left out, the TestBot will be used by default.");
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

        String botName = "TestBot";
        int playerListStart = 1;
        if (args[1].toLowerCase().startsWith("-b:")) {
            botName = args[1].replaceFirst("-b:", "");
            playerListStart = 2;
            if (args.length < 3){
                server.sendServerChat(connId, "You must specify a player name.");
                return;
            }
        }

        String playerName = args[playerListStart];
        for(int i = (playerListStart + 1); i < args.length; i++) {
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
            BotClient c = null;
            if ("Princess".equalsIgnoreCase(botName)) {
                c = new Princess(target.getName(), server.getHost(), server.getPort(), LogLevel.ERROR);
            } else if ("TestBot".equalsIgnoreCase(botName)) {
                c = new TestBot(target.getName(), server.getHost(), server.getPort());
            } else {
                server.sendServerChat(connId, "Unrecognized bot: '" + botName + "'.  Defaulting to TestBot.");
                botName = "TestBot";
                c = new TestBot(target.getName(), server.getHost(), server.getPort());
            }
            c.game.addGameListener(new BotGUI(c));
            try {
                c.connect();
            } catch (Exception e) {
                server.sendServerChat(connId, botName + " failed to connect.");
            }
            c.retrieveServerInfo();
            server.sendServerChat(botName + " has replaced " + target.getName() + ".");
            return;
        }

        server.sendServerChat(connId, "Player " + target.getName()
                + " is not a ghost.");
    }

}
