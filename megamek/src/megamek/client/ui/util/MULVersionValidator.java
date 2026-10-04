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
package megamek.client.ui.util;

import java.awt.Component;
import javax.swing.JOptionPane;

import megamek.SuiteConstants;
import megamek.client.ui.Messages;
import megamek.common.loaders.MULParser;

/**
 * Shared GUI helper that checks the version a {@link MULParser} loaded a MUL from against the currently running
 * version, and prompts the user when there is a mismatch. Call this from any user-initiated MUL file load, after
 * constructing the parser and before using its parsed contents.
 */
public final class MULVersionValidator {

    private MULVersionValidator() {
    }

    /**
     * Checks whether a loaded MUL is version-compatible with the running application, prompting the user when needed.
     * <ul>
     *     <li>If the MUL was made in a newer version, an error dialog is shown, and loading is refused (a newer MUL can
     *     never be loaded in an older version).</li>
     *     <li>If the MUL was made in an older version (or carries no version), the user is asked whether to proceed.</li>
     *     <li>Otherwise loading proceeds silently.</li>
     * </ul>
     *
     * @param parent the parent component for any dialog, which may be {@code null}
     * @param parser the parser that has already loaded the MUL
     *
     * @return {@code true} if loading should proceed, {@code false} if it must be aborted
     */
    public static boolean isCorrectVersion(final Component parent, final MULParser parser) {
        if (parser.isNewerVersion()) {
            final String fileVersion = (parser.getFileVersion() == null) ? "?" : parser.getFileVersion().toString();
            JOptionPane.showMessageDialog(parent,
                  Messages.getString("MULParser.versionWarning.newerVersion", fileVersion, SuiteConstants.VERSION),
                  Messages.getString("MULParser.versionWarning.newerVersion.title"),
                  JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (parser.isOlderVersion()) {
            final String fileVersion = (parser.getFileVersion() == null) ? "?" : parser.getFileVersion().toString();
            final int choice = JOptionPane.showConfirmDialog(parent,
                  Messages.getString("MULParser.versionWarning.olderVersion", fileVersion, SuiteConstants.VERSION),
                  Messages.getString("MULParser.versionWarning.olderVersion.title"),
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.WARNING_MESSAGE);
            return choice == JOptionPane.YES_OPTION;
        }

        return true;
    }
}
