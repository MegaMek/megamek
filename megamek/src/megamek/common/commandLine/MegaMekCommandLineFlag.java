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

public enum MegaMekCommandLineFlag {
    // region Enum Declarations
    // standard game options
    HELP(Messages.getString("MegaMek.Help")),
    DEDICATED(Messages.getString("MegaMek.Help.Dedicated")),
    HOST(Messages.getString("MegaMek.Help.Host")),
    CLIENT(Messages.getString("MegaMek.Help.Client")),
    QUICK(Messages.getFormattedString("MegaMek.Help.Quick", MMConstants.QUICKSAVE_FILE)),

    // exporters and utilities
    EQ_DB(Messages.getString("MegaMek.Help.EquipmentDB")),
    EQE_DB(Messages.getString("MegaMek.Help.EquipmentExtendedDB")),
    EQ_YAML_DB(Messages.getString("MegaMek.Help.EquipmentYamlDB")),
    EQW_DB(Messages.getString("MegaMek.Help.EquipmentWeaponDB")),
    EQA_DB(Messages.getString("MegaMek.Help.EquipmentAmmoDB")),
    EQM_DB(Messages.getString("MegaMek.Help.EquipmentMiscDB")),
    EXPORT(Messages.getString("MegaMek.Help.UnitExport")),
    VALIDATE(Messages.getString("MegaMek.Help.UnitValidator")),
    OUL(Messages.getString("MegaMek.Help.OfficialUnitList")),
    ASC(Messages.getString("MegaMek.Help.UnitAlphastrikeConversion")),
    EDIT_RAT_GEN(Messages.getString("MegaMek.Help.RatgenEdit")),
    DATA_DIR(Messages.getFormattedString("MegaMek.Help.DataDir", Configuration.dataDir())),
    GIF(Messages.getString("MegaMek.Help.Gif"));
    // endregion Enum Declarations

    public final String helpText;

    MegaMekCommandLineFlag(final String helpText) {
        this.helpText = helpText;
    }

    public static MegaMekCommandLineFlag parseFromString(final String text) {
        try {
            return valueOf(text.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            MMLogger.create(MegaMekCommandLineFlag.class)
                  .error("Failed to parse the MegaMekCommandLineFlag from text {}", text);
            throw (ex);
        }
    }
}
