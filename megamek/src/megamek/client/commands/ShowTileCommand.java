/**
 *
 */
package megamek.client.commands;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import megamek.client.Client;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Hex;
import megamek.common.options.OptionsConstants;

/**
 * @author dirk
 *         This command exists to print tile information to the chat
 *         window, it's primarily intended for vissually impaired users.
 */

public class ShowTileCommand extends ClientCommand {
    public final static Set<String> directions = new HashSet<>();
    {
        directions.add("N");
        directions.add("NW");
        directions.add("NE");
        directions.add("S");
        directions.add("SW");
        directions.add("SE");
    }

    public ShowTileCommand(Client client) {
        super(
                client,
                "tile",
                "print the information about a tile into the chat window. " +
                        "Usage: #tile 01 01 [dir1 ...] which would show the details for the hex numbered 01 01. " +
                        "The command can be followed with any number of directions (N,NE,SE,S,SW,NW) to list " +
                        "the tiles following those directions. Updates Current Hex. " +
                        "Can also list just directions to look from current tile.");
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
            Coords coord;
            if ((args.length >= 1) && directions.contains(args[0].toUpperCase())) {
                i = 1;
                coord = getClient().getCurrentHex().translated(args[0]);
            } else if ((args.length > 1) && directions.contains(args[1].toUpperCase()) ) {
                i = 2;
                coord = getClient().getCurrentHex().translated(args[1]);
            } else {
                coord = new Coords(Integer.parseInt(args[1]) - 1, Integer
                        .parseInt(args[2]) - 1);
            }
            Hex hex;

            do {
                hex = getClient().getGame().getBoard().getHex(coord);
                getClient().setCurrentHex(hex);
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
            } while (i <= args.length);

            return report;
        } catch (NumberFormatException nfe) {
        } catch (NullPointerException npe) {
        } catch (IndexOutOfBoundsException ioobe) {
        }

        return "Error parsing the command.";
    }
}
