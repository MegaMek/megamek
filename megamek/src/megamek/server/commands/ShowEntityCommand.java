package megamek.server.commands;

import megamek.common.Entity;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * This command exists to print entity information to the chat window.
 * It is primarily intended for visually impaired users.
 * @author dirk
 */
public class ShowEntityCommand extends ServerCommand {

    private final GameManager gameManager;

    public ShowEntityCommand(Server server, GameManager gameManager) {
        super(server, "entity",
                "Print the information about a entity into the chat window. Usage: /entity # which would show the details for the entity numbered #.");
        // to be extended by adding /entity unit# loc# to list details on locations.
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     * 
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String... args) {
        if (server.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
            server.sendServerChat(connId, "Sorry, this command is disabled during double blind.");
            return;
        }
        try {
            int id = Integer.parseInt(args[1]);
            Entity ent = gameManager.getGame().getEntity(id);

            if (ent != null) {
                server.sendServerChat(connId, ent.statusToString());
            } else {
                server.sendServerChat(connId, "No such entity.");
            }
        } catch (Exception ignored) {

        }
    }
}
