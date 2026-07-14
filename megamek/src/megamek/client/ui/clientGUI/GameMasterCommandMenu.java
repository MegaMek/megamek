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
import megamek.server.commands.ChangeOwnershipCommand;
import megamek.server.commands.ChangeWeatherCommand;
import megamek.server.commands.ClientServerCommand;
import megamek.server.commands.DisasterCommand;
import megamek.server.commands.EndGameCommand;
import megamek.server.commands.FirefightCommand;
import megamek.server.commands.FirestarterCommand;
import megamek.server.commands.FirestormCommand;
import megamek.server.commands.KillCommand;
import megamek.server.commands.NoFiresCommand;
import megamek.server.commands.OrbitalBombardmentCommand;
import megamek.server.commands.RemoveSmokeCommand;
import megamek.server.commands.RescueCommand;

/**
 * Builds the Game Master special commands menu: one entry per server command that only a Game Master may run, each
 * opening the {@link ClientCommandDialog} that builds an input form from the command's own argument definitions.
 *
 * <p>The menu is shared by the board context menu ({@link MapMenu}), which passes the hex that was clicked, and the
 * game commands strip ({@link GameCommandsMenu}), which has no hex context and passes {@code null}. Keeping one list
 * means a command added here shows up in both places.</p>
 */
public final class GameMasterCommandMenu {

    private GameMasterCommandMenu() {
    }

    /**
     * Builds the Game Master special commands menu. The caller is responsible for only offering it to a player who
     * actually holds the Game Master role - the server rejects these commands from anyone else.
     *
     * @param clientGUI The client GUI the command dialogs are shown on
     * @param coords    The hex the command should default to, or {@code null} when the command is not tied to a hex
     *
     * @return The Game Master special commands menu
     */
    public static JMenu createSpecialCommandsMenu(ClientGUI clientGUI, @Nullable Coords coords) {
        JMenu menu = new JMenu(Messages.getString("Gamemaster.SpecialCommands"));
        for (ClientServerCommand command : gameMasterCommands()) {
            JMenuItem commandItem = new JMenuItem(command.getLongName());
            commandItem.addActionListener(evt ->
                  new ClientCommandDialog(clientGUI.getFrame(), clientGUI, command, coords).setVisible(true));
            menu.add(commandItem);
        }
        return menu;
    }

    /**
     * Creates the Game Master commands the menu offers. The commands are built without a server or game manager,
     * because only their name, help text and argument definitions are read here; the command itself is run by the
     * server after the dialog sends it as a chat command.
     *
     * @return The Game Master commands, in menu order
     */
    private static List<ClientServerCommand> gameMasterCommands() {
        return List.of(new ChangeOwnershipCommand(null, null),
              new ChangeWeatherCommand(null, null),
              new DisasterCommand(null, null),
              new EndGameCommand(null, null),
              new KillCommand(null, null),
              new FirefightCommand(null, null),
              new FirestarterCommand(null, null),
              new FirestormCommand(null, null),
              new NoFiresCommand(null, null),
              new OrbitalBombardmentCommand(null, null),
              new RemoveSmokeCommand(null, null),
              new RescueCommand(null, null));
    }
}
