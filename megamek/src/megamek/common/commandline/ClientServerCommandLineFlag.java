/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved
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
package megamek.common.commandline;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.common.Configuration;
import org.apache.logging.log4j.LogManager;

import java.util.Locale;

public enum ClientServerCommandLineFlag {
    //region Enum Declarations
    HELP(Messages.getString("MegaMek.Help")),
    USEDEFAULTS(Messages.getString("MegaMek.Help.UseDefaults")),
    PORT(Messages.getFormattedString("MegaMek.Help.Port", MMConstants.MIN_PORT, MMConstants.MAX_PORT, MMConstants.DEFAULT_PORT)),
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
    //endregion Enum Declarations

    private final String helpText;
    private final boolean serverArg;
    private final boolean clientArg;
    private final boolean hostArg;

    //region Constructors
    ClientServerCommandLineFlag(final String helpText) {
        this(helpText, true, true, true);
    }

    ClientServerCommandLineFlag(final String helpText, boolean serverArg, boolean clientArg, boolean hostArg) {
        this.helpText = helpText;
        this.serverArg = serverArg;
        this.clientArg = clientArg;
        this.hostArg = hostArg;
    }
    //endregion Constructors

    public static ClientServerCommandLineFlag parseFromString(final String text) {
        try {
            return valueOf(text.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            LogManager.getLogger().error(String.format("Failed to parse the ClientServerCommandLineFlag from '%s'", text));
            throw(ex);
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
