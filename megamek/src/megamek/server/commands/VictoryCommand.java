/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Player;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Causes automatic victory at the end of the current turn.
 *
 * @author Ben
 * @since July 11, 2002, 2:24 PM
 */
public class VictoryCommand extends ServerCommand {

    public static final String commandName = "victory";
    public static final String helpText = "Causes automatic victory for the issuing player or his/her team at the " +
          "end of this turn. Must be acknowledged by all opponents using the " +
          "/defeat command. Usage: /victory <password>";
    public static final String restrictedUse = "Observers are restricted from declaring victory.";
    public static final String badPassword = "The password is incorrect.  Usage: /victory <password>";
    private static final String declareIndividual = " declares individual victory at the end of the turn. This must " +
          "be acknowledged by all opponents using the /defeat command or " +
          "no victory will occur.";
    private static final String declareTeam = " declares team victory at the end of the turn. This must be " +
          "acknowledged by all opponents using the /defeat command or no " +
          "victory will occur.";

    private final TWGameManager gameManager;

    /**
     * Creates new VictoryCommand
     */
    public VictoryCommand(Server server, TWGameManager gameManager) {
        super(server, commandName, helpText);
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId, restrictedUse);
            return;
        }

        if (!server.isPassworded()
              || (args.length > 1 && server.isPassword(args[1]))) {
            reset(connId);
        } else {
            server.sendServerChat(connId, badPassword);
        }
    }

    public static String getDeclareIndividual(String playerName) {
        return playerName + declareIndividual;
    }

    public static String getDeclareTeam(String playerName) {
        return playerName + declareTeam;
    }

    private void reset(int connId) {
        Player player = server.getPlayer(connId);

        if (player.getTeam() == Player.TEAM_NONE) {
            server.sendServerChat(getDeclareIndividual(player.getName()));
        } else {
            server.sendServerChat(getDeclareTeam(player.getName()));
        }
        gameManager.forceVictory(player, false, false);
    }

}
