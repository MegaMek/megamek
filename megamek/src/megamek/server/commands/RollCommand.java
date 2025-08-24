/*
  Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.server.commands;

import megamek.common.compute.Compute;
import megamek.server.Server;

/**
 * @author Ben
 * @since 11:53 PM, April 18, 2002
 */
public class RollCommand extends ServerCommand {

    /** Creates new RollCommand */
    public RollCommand(Server server) {
        super(server, "roll", "Rolls some dice.  Usage: /roll [XdY]");
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        int dice = 2;
        int sides = 6;
        try {
            if (args.length > 1) {
                int d = args[1].indexOf('d');
                dice = Integer.parseInt(args[1].substring(0, d));
                sides = Integer.parseInt(args[1].substring(d + 1));
            }
        } catch (Exception ex) {
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
