/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.commandLine;

import java.util.Locale;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.common.Configuration;
import megamek.logging.MMLogger;

public enum ClientServerCommandLineFlag {
    // region Enum Declarations
    HELP(Messages.getString("MegaMek.Help")),
    USEDEFAULTS(Messages.getString("MegaMek.Help.UseDefaults")),
    PORT(Messages.getFormattedString("MegaMek.Help.Port", MMConstants.MIN_PORT, MMConstants.MAX_PORT,
          MMConstants.DEFAULT_PORT)),
    DATADIR(Messages.getFormattedString("MegaMek.Help.DataDir", Configuration.dataDir())),
    // server or host only options
    ANNOUNCE(Messages.getString("MegaMek.Help.Announce"), true, false, true),
    MAIL(Messages.getString("MegaMek.Help.Mail"), true, false, true),
    SAVEGAME(Messages.getString("MegaMek.Help.SaveGame"), true, false, true),
    PASSWORD(Messages.getString("MegaMek.Help.Password"), true, false, true),
    // client or host only options
    PLAYERNAME(Messages.getString("MegaMek.Help.PlayerName"), false, true, true),
    // client only options
    SERVER(Messages.getFormattedString("MegaMek.Help.Server", MMConstants.LOCALHOST), false, true, false);
    // endregion Enum Declarations

    private final String helpText;
    private final boolean serverArg;
    private final boolean clientArg;
    private final boolean hostArg;

    // region Constructors
    ClientServerCommandLineFlag(final String helpText) {
        this(helpText, true, true, true);
    }

    ClientServerCommandLineFlag(final String helpText, boolean serverArg, boolean clientArg, boolean hostArg) {
        this.helpText = helpText;
        this.serverArg = serverArg;
        this.clientArg = clientArg;
        this.hostArg = hostArg;
    }
    // endregion Constructors

    public static ClientServerCommandLineFlag parseFromString(final String text) {
        try {
            return valueOf(text.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            MMLogger.create(ClientServerCommandLineFlag.class)
                  .error(String.format("Failed to parse the ClientServerCommandLineFlag from '%s'", text));
            throw (ex);
        }
    }

    public boolean isServerArg() {
        return serverArg;
    }

    public String getHelpText() {
        return helpText;
    }

    public boolean isClientArg() {
        return clientArg;
    }

    public boolean isHostArg() {
        return hostArg;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
