/*
 * Copyright (c) 2022-2024 - The MegaMek Team. All Rights Reserved
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

import java.util.Locale;

import org.apache.logging.log4j.LogManager;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.common.Configuration;

public enum MegaMekCommandLineFlag {
    // region Enum Declarations
    // standard game options
    HELP(Messages.getString("MegaMek.Help")),
    DEDICATED(Messages.getString("MegaMek.Help.Dedicated")),
    HOST(Messages.getString("MegaMek.Help.Host")),
    CLIENT(Messages.getString("MegaMek.Help.Client")),
    QUICK(Messages.getFormattedString("MegaMek.Help.Quick", MMConstants.QUICKSAVE_FILE)),

    // exporters and utilities
    EQDB(Messages.getString("MegaMek.Help.EquipmentDB")),
    EQEDB(Messages.getString("MegaMek.Help.EquipmentExtendedDB")),
    EQWDB(Messages.getString("MegaMek.Help.EquipmentWeaponDB")),
    EQADB(Messages.getString("MegaMek.Help.EquipmentAmmoDB")),
    EQMDB(Messages.getString("MegaMek.Help.EquipmentMiscDB")),
    EXPORT(Messages.getString("MegaMek.Help.UnitExport")),
    VALIDATE(Messages.getString("MegaMek.Help.UnitValidator")),
    OUL(Messages.getString("MegaMek.Help.OfficialUnitList")),
    ASC(Messages.getString("MegaMek.Help.UnitAlphastrikeConversion")),
    EDITRATGEN(Messages.getString("MegaMek.Help.RatgenEdit")),
    DATADIR(Messages.getFormattedString("MegaMek.Help.DataDir", Configuration.dataDir()));
    // endregion Enum Declarations

    public final String helpText;

    MegaMekCommandLineFlag(final String helpText) {
        this.helpText = helpText;
    }

    public static MegaMekCommandLineFlag parseFromString(final String text) {
        try {
            return valueOf(text.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to parse the MegaMekCommandLineFlag from text " + text);
            throw (ex);
        }
    }
}
