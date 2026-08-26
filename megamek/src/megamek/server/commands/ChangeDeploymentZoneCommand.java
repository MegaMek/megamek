/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.interfaces.IStartingPositions;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.commands.arguments.PlayerArgument;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Sets which edge of the board a player's units arrive from.
 *
 * <p>A player who never went through the lobby has no deployment zone of their own, which means "anywhere" - so
 * someone brought into a running game could walk on in the middle of the enemy line. This is how a gamemaster gives
 * them an edge, the way it would have been set in the lobby.</p>
 *
 * <p>The zone is only read when a unit deploys, so setting it part way through a game affects whatever has not
 * arrived yet and leaves anything already on the board where it stands.</p>
 */
public class ChangeDeploymentZoneCommand extends GamemasterServerCommand {

    private static final MMLogger LOGGER = MMLogger.create(ChangeDeploymentZoneCommand.class);

    public static final String PLAYER_ID = "playerID";
    public static final String ZONE_ID = "zoneID";

    /** The first zone in the table, which is "anywhere on the board". */
    private static final int FIRST_ZONE = 0;

    public ChangeDeploymentZoneCommand(Server server, TWGameManager gameManager) {
        super(server,
              gameManager,
              "changeDeploymentZone",
              Messages.getString("Gamemaster.cmd.changeDeploymentZone.help"),
              Messages.getString("Gamemaster.cmd.changeDeploymentZone.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
              new PlayerArgument(PLAYER_ID, Messages.getString("Gamemaster.cmd.changeDeploymentZone.playerID")),
              new IntegerArgument(ZONE_ID,
                    Messages.getString("Gamemaster.cmd.changeDeploymentZone.zoneID"),
                    FIRST_ZONE,
                    IStartingPositions.START_LOCATION_NAMES.length - 1));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        int playerId = ((PlayerArgument) args.get(PLAYER_ID)).getValue();
        int zoneId = ((IntegerArgument) args.get(ZONE_ID)).getValue();

        Player player = server.getGame().getPlayer(playerId);
        if (player == null) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.changeDeploymentZone.playerNotFound"));
            return;
        }
        if ((zoneId < FIRST_ZONE) || (zoneId >= IStartingPositions.START_LOCATION_NAMES.length)) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.changeDeploymentZone.noSuchZone"));
            return;
        }

        String zoneName = IStartingPositions.START_LOCATION_NAMES[zoneId];
        gameManager.setStartingPosition(player, zoneId);

        server.sendServerChat(Messages.getString("Gamemaster.cmd.changeDeploymentZone.success",
              player.getName(), zoneName));
        LOGGER.info("[GMPlayerSetup] {} set the deployment zone of {} to {} ({})",
              server.getPlayer(connId).getName(), player.getName(), zoneName, zoneId);
    }
}
