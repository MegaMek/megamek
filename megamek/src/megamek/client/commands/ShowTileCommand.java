/**
 *
 */
package megamek.client.commands;

import java.util.Iterator;

import megamek.client.Client;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IHex;
import megamek.common.options.OptionsConstants;

/**
 * @author dirk
 *         This command exists to print tile information to the chat
 *         window, it's primarily intended for vissually impaired users.
 */

public class ShowTileCommand extends ClientCommand {

    public ShowTileCommand(Client client) {
        super(
                client,
                "tile",
                "print the information about a tile into the chat window. Ussage: #tile 01 01 [dir1 ...] which would show the details for the hex numbered 01 01. The command can be followed with any number of directions (N,NE,SE,S,SW,NW) to list the tiles following those diretions.");
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public String run(String[] args) {
        try {
            int i = 3;
            String str = "", report = "";
            Coords coord = new Coords(Integer.parseInt(args[1]) - 1, Integer
                                                                             .parseInt(args[2]) - 1);
            IHex hex;

            do {
                hex = getClient().getGame().getBoard().getHex(coord);
                if (hex != null) {
                    str = "Details for hex (" + (coord.getX() + 1) + ", "
                          + (coord.getY() + 1) + ") : " + hex.toString();

                    // if we are not playing in double blind mode also list the
                    // units in this tile.
                    if (!getClient().getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)) {
                        Iterator<Entity> entList = getClient().getGame()
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

                    report = report + str + "\n";
                } else {
                    report = report + "Hex (" + (coord.getX() + 1) + ", "
                             + (coord.getY() + 1) + ") is not on the board.\n";
                }

                if (i < args.length) {
                    coord = coord.translated(args[i]);
                }

                i++;
            } while (i < args.length);

            return report;
        } catch (NumberFormatException nfe) {
        } catch (NullPointerException npe) {
        } catch (IndexOutOfBoundsException ioobe) {
        }

        return "Error parsing the command.";
    }

}
