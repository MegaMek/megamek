/**
 * 
 */
package megamek.client.commands;

import java.util.Enumeration;

import megamek.client.Client;
import megamek.common.Entity;

/**
 * @author dirk 
 * This command exists to print entity information to the chat
 * window, it's primarily intended for vissually impaired users.
 */

public class ShowEntityCommand extends ClientCommand {

    public ShowEntityCommand(Client client) {
        super(
                client,
                "entity",
                "print the information about a entity into the chat window. Ussage: #entity 5 whih would show the details for the entity numbered 5. Also #entity 5 0 would show location 0 of entity 5.");
        // to be extended by adding /entity unit# loc# to list details on
        // locations.
    }

    /**
     * Run this command with the arguments supplied
     * 
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public String run(String[] args) {
        // is this nessesary to prevent cheating?
        if (client.game.getOptions().booleanOption("double_blind")) {
            return "Sorry, this command is disabled during double blind.";
        }
        if (args.length == 1) {
            String list = "List of all entities.\n";
            Enumeration<Entity> entities = client.getEntities();
            while (entities.hasMoreElements()) {
                Entity ent = entities.nextElement();
                list += ent.getId() + " " + ent.getOwner().getName() + "'s "
                        + ent.getDisplayName() + "\n";
            }
            return list;
        }
        try {
            int id = Integer.parseInt(args[1]);
            Entity ent = client.getEntity(id);

            if (ent != null) {
                if (args.length > 2) {
                    String str = "";
                    for (int i = 2; i < args.length; i++) {
                        str += ent.statusToString(args[i]);
                    }
                    return str;
                }
                return ent.statusToString();
            } else {
                return "No such entity.";
            }
        } catch (NumberFormatException nfe) {
        } catch (NullPointerException npe) {
        } catch (IndexOutOfBoundsException ioobe) {
        }

        return "Error parsing the command.";
    }

}
