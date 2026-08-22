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

package megamek.client.ui.clientGUI;

import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.ClientCommandDialog;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.logging.MMLogger;
import megamek.server.commands.BuildingCommand;
import megamek.server.commands.ChangeWeatherCommand;
import megamek.server.commands.ClientServerCommand;
import megamek.server.commands.DisasterCommand;
import megamek.server.commands.FirefightCommand;
import megamek.server.commands.FirestarterCommand;
import megamek.server.commands.FirestormCommand;
import megamek.server.commands.NoFiresCommand;
import megamek.server.commands.OrbitalBombardmentCommand;
import megamek.server.commands.RemoveSmokeCommand;
import megamek.server.commands.TerrainCommand;

/**
 * Builds the Game Master special commands menu: one entry per server command that only a Game Master may run, each
 * opening the {@link ClientCommandDialog} that builds an input form from the command's own argument definitions.
 *
 * <p>The menu is shared by the board context menu ({@link MapMenu}), which passes the hex that was clicked, and the
 * game commands strip ({@link GameCommandsMenu}), which has no hex context and passes {@code null}. Each gets the
 * commands that match how it was opened, and nothing else.</p>
 *
 * <p>Right-clicking a hex means "do something to this hex", so that menu offers only the commands that act on one
 * hex, already aimed at the hex that was clicked. The commands that act on the whole map are not about the hex under
 * the cursor at all, so they belong on the game commands strip instead, where no hex is implied.</p>
 *
 * <p>Commands that act on a single unit are in neither list. They live in that unit's Edit Damage dialog, which is
 * reached from the same context menu and shows the unit's current state while it changes it.</p>
 */
public final class GameMasterCommandMenu {
    private static final MMLogger LOGGER = MMLogger.create(GameMasterCommandMenu.class);

    private GameMasterCommandMenu() {
    }

    /**
     * Builds the Game Master special commands menu. The caller is responsible for only offering it to a player who
     * actually holds the Game Master role - the server rejects these commands from anyone else.
     *
     * @param clientGUI The client GUI the command dialogs are shown on
     * @param coords    The hex the commands should act on, or {@code null} when the menu is opened without a hex,
     *                  which limits it to the board-wide commands
     *
     * @return The Game Master special commands menu
     */
    public static JMenu createSpecialCommandsMenu(ClientGUI clientGUI, @Nullable Coords coords) {
        JMenu menu = new JMenu(Messages.getString("Gamemaster.SpecialCommands"));
        for (ClientServerCommand command : commandsFor(coords)) {
            JMenuItem commandItem = new JMenuItem(command.getLongName());
            commandItem.addActionListener(event ->
                  new ClientCommandDialog(clientGUI.getFrame(), clientGUI, command, coords).setVisible(true));
            menu.add(commandItem);
        }
        return menu;
    }

    /**
     * The commands to offer, chosen by how the menu was opened. Opened on a hex it offers the commands that act on
     * that hex; opened without one it offers the commands that act on the whole map.
     *
     * <p>Package-private so the rule itself can be tested: a command offered without a hex must not declare a hex or
     * unit argument, because there would be nothing to fill it in from.</p>
     *
     * @param coords The hex the menu was opened on, or {@code null} when it was opened without one
     *
     * @return The commands to offer, in menu order
     */
    static List<ClientServerCommand> commandsFor(@Nullable Coords coords) {
        if (coords == null) {
            LOGGER.debug("[GMCommands] menu opened without a hex - offering the board-wide commands");
            return boardWideCommands();
        }
        LOGGER.debug("[GMCommands] menu opened on hex {} - offering the hex commands", coords.getBoardNum());
        return hexTargetedCommands();
    }

    /**
     * Creates the Game Master commands that act on the whole map. They are offered from the game commands strip,
     * which implies no hex, rather than from a right-click on one hex in particular.
     *
     * <p>The commands are built without a server or game manager, because only their name, help text and argument
     * definitions are read here; the command itself is run by the server after the dialog sends it as a chat
     * command.</p>
     *
     * @return The board-wide Game Master commands, in menu order
     */
    private static List<ClientServerCommand> boardWideCommands() {
        return List.of(new ChangeWeatherCommand(null, null),
              new DisasterCommand(null, null),
              new FirestormCommand(null, null),
              new NoFiresCommand(null, null),
              new RemoveSmokeCommand(null, null));
    }

    /**
     * Creates the Game Master commands that act on one hex. Each takes an X and a Y coordinate, which the dialog fills
     * in from the hex that was right-clicked, so these are only offered from the board context menu.
     *
     * @return The hex-targeted Game Master commands, in menu order
     */
    private static List<ClientServerCommand> hexTargetedCommands() {
        return List.of(new BuildingCommand(null, null),
              new FirefightCommand(null, null),
              new FirestarterCommand(null, null),
              new OrbitalBombardmentCommand(null, null),
              new TerrainCommand(null, null));
    }
}
