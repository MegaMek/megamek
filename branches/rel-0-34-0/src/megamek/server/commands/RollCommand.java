/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * RollCommand.java
 *
 * Created on April 18, 2002, 11:53 PM
 */

package megamek.server.commands;

import megamek.common.Compute;
import megamek.server.Server;

/**
 * @author Ben
 * @version
 */
public class RollCommand extends ServerCommand {

    /** Creates new RollCommand */
    public RollCommand(Server server) {
        super(server, "roll", "Rolls some dice.  Usage: /roll [XdY]");
    }

    /**
     * Run this command with the arguments supplied
     */
    public void run(int connId, String[] args) {
        int dice = 2;
        int sides = 6;
        try {
            if (args.length > 1) {
                int d = args[1].indexOf('d');
                dice = Integer.parseInt(args[1].substring(0, d));
                sides = Integer.parseInt(args[1].substring(d + 1));
            }
        } catch (NumberFormatException ex) {
            server.sendServerChat(connId, "/roll: error parsing arguments.");
            return;
        } catch (StringIndexOutOfBoundsException ex) {
            server.sendServerChat(connId, "/roll: error parsing arguments.");
            return;
        }
        roll(connId, dice, sides);
    }

    private void roll(int connId, int dice, int sides) {
        StringBuffer diceBuffer = new StringBuffer();
        int total = 0;
        for (int i = 0; i < dice; i++) {
            int roll = Compute.randomInt(sides) + 1;
            total += roll;

            // for one die, we're all set
            if (dice < 2) {
                diceBuffer.append(roll);
                continue;
            }

            // 2+ dice, use commas and "and"
            if (i < dice - 1) {
                diceBuffer.append(roll);
                diceBuffer.append(", ");
            } else {
                diceBuffer.append("and ");
                diceBuffer.append(roll);
            }
        }
        server.sendServerChat(server.getPlayer(connId).getName()
                + " has rolled " + diceBuffer + " for a total of " + total
                + ", using " + dice + "d" + sides);
    }
}
