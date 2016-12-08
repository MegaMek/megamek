/**
 *
 */
package megamek.server.commands;

import java.util.Iterator;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IHex;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author dirk This command exists to print tile information to the chat
 *         window, it's primarily intended for vissually impaired users.
 */

public class ShowTileCommand extends ServerCommand {

    public ShowTileCommand(Server server) {
        super(
                server,
                "tile",
                "print the information about a tile into the chat window. Ussage: /tile 01 01 whih would show the details for the hex numbered 01 01.");
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        try {
            int i = 3;
            String str = "";
            Coords coord = new Coords(Integer.parseInt(args[1]) - 1, Integer
                                                                             .parseInt(args[2]) - 1);
            IHex hex;

            do {
                hex = server.getGame().getBoard().getHex(coord);
                if (hex != null) {
                    str = "Details for hex (" + (coord.getX() + 1) + ", "
                          + (coord.getY() + 1) + ") : " + hex.toString();

                    // if we are not playing in double blind mode also list the
                    // units in this tile.
                    if (!server.getGame().getOptions().booleanOption(
                            OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
                        Iterator<Entity> entList = server.getGame()
                                                         .getEntities(coord);
                        if (entList.hasNext()) {
                            str = str + "; Contains entities: "
                                  + entList.next().getId();
                            while (entList.hasNext()) {
                                str = str + ", "
                                      + entList.next().getId();
                            }
                        }
                    }

                    server.sendServerChat(connId, str);
                } else {
                    server.sendServerChat(connId, "Hex (" + (coord.getX() + 1)
                                                  + ", " + (coord.getY() + 1) + ") is not on the board.");
                }

                if (i < args.length) {
                    coord = coord.translated(args[i]);
                }

                i++;
            } while (i < args.length);
        } catch (NumberFormatException nfe) {
        } catch (NullPointerException npe) {
        } catch (IndexOutOfBoundsException ioobe) {
        }
    }

}
